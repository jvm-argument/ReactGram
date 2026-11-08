/*
 * ReactGram saved messages.
 *
 * Concept taken from AyuGram for Android ("save deleted messages" / "edits history"), Copyright @Radolyn, 2023,
 * https://github.com/AyuGram/AyuGram4A, GPL-2.0-or-later (see LICENSE). Implementation written for ReactGram: instead of
 * copying messages into a second database, messages deleted by the other side stay in Telegram's own database and are
 * only flagged here; edits keep the previous text.
 */

package com.reactgram.features;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class SavedStore extends SQLiteOpenHelper {
    private static SavedStore instance;

    /** One previous version of an edited message. */
    public static final class Edit {
        public final int editDate;
        public final String text;

        Edit(int editDate, String text) {
            this.editDate = editDate;
            this.text = text;
        }
    }

    private final ArrayList<Runnable> listeners = new ArrayList<>();
    private final HashSet<String> deletedCache = new HashSet<>();
    private final HashSet<String> editedCache = new HashSet<>();
    private boolean cachesLoaded;

    private SavedStore(Context context) {
        super(context, "reactgram_saved.db", null, 1);
    }

    public static synchronized SavedStore get() {
        if (instance == null) {
            instance = new SavedStore(ApplicationLoader.applicationContext);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE deleted (account INTEGER, dialog INTEGER, mid INTEGER, at INTEGER, PRIMARY KEY (account, dialog, mid))");
        db.execSQL("CREATE TABLE edits (id INTEGER PRIMARY KEY AUTOINCREMENT, account INTEGER, dialog INTEGER, mid INTEGER, edit_date INTEGER, saved_at INTEGER, text TEXT)");
        db.execSQL("CREATE INDEX edits_message ON edits (account, dialog, mid)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    /** Listeners are called on the UI thread whenever deleted flags change. */
    public synchronized void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyChanged() {
        final ArrayList<Runnable> copy;
        synchronized (this) {
            copy = new ArrayList<>(listeners);
        }
        org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> {
            for (Runnable r : copy) {
                r.run();
            }
        });
    }

    private static String key(int account, long dialog, int mid) {
        return account + ":" + dialog + ":" + mid;
    }

    private synchronized void ensureCaches() {
        if (cachesLoaded) {
            return;
        }
        try {
            final SQLiteDatabase db = getReadableDatabase();
            try (Cursor c = db.rawQuery("SELECT account, dialog, mid FROM deleted", null)) {
                while (c.moveToNext()) {
                    deletedCache.add(key(c.getInt(0), c.getLong(1), c.getInt(2)));
                }
            }
            try (Cursor c = db.rawQuery("SELECT DISTINCT account, dialog, mid FROM edits", null)) {
                while (c.moveToNext()) {
                    editedCache.add(key(c.getInt(0), c.getLong(1), c.getInt(2)));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        cachesLoaded = true;
    }

    /** Flags messages as deleted by the other side. {@code dialog} is 0 when the update carries no dialog. */
    public synchronized void markDeleted(int account, long dialog, List<Integer> ids) {
        ensureCaches();
        try {
            final SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                final long now = System.currentTimeMillis() / 1000;
                for (int id : ids) {
                    final ContentValues v = new ContentValues();
                    v.put("account", account);
                    v.put("dialog", dialog);
                    v.put("mid", id);
                    v.put("at", now);
                    db.insertWithOnConflict("deleted", null, v, SQLiteDatabase.CONFLICT_IGNORE);
                    deletedCache.add(key(account, dialog, id));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        notifyChanged();
    }

    public synchronized boolean isDeleted(int account, long dialog, int mid) {
        ensureCaches();
        return deletedCache.contains(key(account, dialog, mid)) || deletedCache.contains(key(account, 0, mid));
    }

    /** Stores the text a message had before it was edited. */
    public synchronized void recordEdit(int account, long dialog, TLRPC.Message old) {
        if (old == null || old.message == null) {
            return;
        }
        ensureCaches();
        try {
            final List<Edit> existing = getEdits(account, dialog, old.id);
            if (!existing.isEmpty() && existing.get(existing.size() - 1).text.equals(old.message)) {
                return;
            }
            final ContentValues v = new ContentValues();
            v.put("account", account);
            v.put("dialog", dialog);
            v.put("mid", old.id);
            v.put("edit_date", old.edit_date != 0 ? old.edit_date : old.date);
            v.put("saved_at", System.currentTimeMillis() / 1000);
            v.put("text", old.message);
            getWritableDatabase().insert("edits", null, v);
            editedCache.add(key(account, dialog, old.id));
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public synchronized boolean hasEdits(int account, long dialog, int mid) {
        ensureCaches();
        return editedCache.contains(key(account, dialog, mid));
    }

    public synchronized List<Edit> getEdits(int account, long dialog, int mid) {
        final ArrayList<Edit> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
            "SELECT edit_date, text FROM edits WHERE account = ? AND dialog = ? AND mid = ? ORDER BY id",
            new String[]{String.valueOf(account), String.valueOf(dialog), String.valueOf(mid)})) {
            while (c.moveToNext()) {
                result.add(new Edit(c.getInt(0), c.getString(1)));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }
}
