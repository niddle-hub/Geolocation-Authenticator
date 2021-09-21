package com.example.myapplication

import androidx.room.*

@Dao
interface EntityDao {
    @Query("SELECT * FROM entity")
    fun getAll(): List<Entity>

    @Query("DELETE FROM entity")
    fun deleteAll()

    @Query("SELECT * FROM entity WHERE id = :id")
    fun getById(id: Long): Entity

    @Insert
    fun insert(entity: Entity)

    @Insert
    fun insert(entity: List<Entity>)

    @Update
    fun update(entity: Entity)

    @Delete
    fun delete(entity: Entity)
}