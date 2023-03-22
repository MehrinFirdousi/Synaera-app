package com.example.synaera

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


open class DatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table loggedin ($EMAIL TEXT NOT NULL PRIMARY KEY, $NAME TEXT NOT NULL, $PASSWORD TEXT NOT NULL, id INTEGER NOT NULL);")
        db.execSQL(CREATE_TABLE)

        db.execSQL("INSERT INTO $TABLE_NAME ($EMAIL, $NAME, $PASSWORD) VALUES ('qusai061@gmail.com', 'Qusai', '1')")


    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS loggedin")
        onCreate(db)
    }

    companion object {
        // Table Name
        const val TABLE_NAME = "COUNTRIES"

        // Table columns
        const val ID = "rowid"
        const val EMAIL = "email"
        const val NAME = "name"
        const val PASSWORD = "password"

        // Database Information
        const val DB_NAME = "SYNAERA.DB"

        // database version
        const val DB_VERSION = 1

        // Creating table query
        private const val CREATE_TABLE = ("create table $TABLE_NAME($EMAIL " +
                "TEXT NOT NULL PRIMARY KEY, $NAME TEXT NOT NULL, $PASSWORD TEXT NOT NULL);")
    }

    open fun addUser(user: User) {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(EMAIL, user.email)
        values.put(NAME, user.name)
        values.put(PASSWORD, user.password)

        db.insert(TABLE_NAME, null, values)
        db.close()

    }

    open fun addLoggedInUser(user: User) {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(EMAIL, user.email)
        values.put(NAME, user.name)
        values.put(PASSWORD, user.password)
        values.put("id", user.id)

        db.insert("loggedin", null, values)
        db.close()

    }

    open fun drop() {
        val db = this.writableDatabase

        onUpgrade(db, DB_VERSION, 1)
    }

    open fun getUser(email: String): User {
        val db = this.readableDatabase

        val cursor: Cursor? = db.query(
            TABLE_NAME,
            arrayOf(
                ID,
                EMAIL,
                NAME, PASSWORD
            ),
            "$EMAIL=?",
            arrayOf(email),
            null,
            null,
            null,
            null
        )
        cursor!!.moveToFirst()
        val user = User(cursor.getInt(0),cursor.getString(1), cursor.getString(2), cursor.getString(3))
        cursor.close()
        return user

    }

    open fun getUser(id: Int): User {
        val db = this.readableDatabase

        val cursor: Cursor? = db.query(
            TABLE_NAME,
            arrayOf(
                ID,
                EMAIL,
                NAME, PASSWORD
            ),
            "$ID=?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            null
        )
        cursor!!.moveToFirst()
        val user = User(cursor.getInt(0),cursor.getString(1), cursor.getString(2), cursor.getString(3))
        cursor.close()
        return user

    }

    fun getAllUsers() : ArrayList<User> {
        val userList = ArrayList<User>()
        // Select All Query
        val selectQuery = "SELECT $ID, $EMAIL, $NAME, $PASSWORD FROM $TABLE_NAME"

        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val user = User(cursor.getInt(0),cursor.getString(1), cursor.getString(2),
                    cursor.getString(3))

                userList.add(user)
            } while (cursor.moveToNext())

            cursor.close()
        }
        return userList
    }

    fun getLoggedIn() : User {
        var user = User (0,"", "" , "")
        // Select All Query
        val selectQuery = "SELECT * FROM loggedin"

        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {

            user = User(
                cursor.getInt(3), cursor.getString(0),
                cursor.getString(1), cursor.getString(2)
            )
        }

        cursor.close()
        return user
    }

    open fun updateUser(user : User, id : Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(NAME, user.name)
        values.put(EMAIL, user.email)

        // updating row
        db.update(
            TABLE_NAME,
            values,
            "$ID = ?",
            arrayOf(id.toString())
        )

        db.close()

    }


    open fun deleteUser(user: User) {
        val db = this.writableDatabase
        db.delete(
            TABLE_NAME,
            "$ID = ?",
            arrayOf(user.id.toString())
        )
        db.close()
    }

    open fun deleteLoggedInUser(user: User) {
        val db = this.writableDatabase
        db.delete(
            "loggedin",
            "$EMAIL = ?",
            arrayOf(user.email)
        )
        db.close()
    }


    open fun getUsersCount(): Int {
        val countQuery = "SELECT * FROM $TABLE_NAME"
        val db = this.readableDatabase
        val cursor = db.rawQuery(countQuery, null)
        cursor.close()

        // return count
        return cursor.count
    }
}