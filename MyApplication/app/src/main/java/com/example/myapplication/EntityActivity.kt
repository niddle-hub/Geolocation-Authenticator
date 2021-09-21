package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.toxicbakery.bcrypt.Bcrypt
import kotlin.random.Random

class EntityActivity : AppCompatActivity() {

    private lateinit var addBtn: Button
    private lateinit var getBtn: Button
    private lateinit var remBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entity)

        val entityDao: EntityDao = (applicationContext as AppDelegate)
            .getAppDatabase()
            .entityDao()

        addBtn = findViewById(R.id.add)
        addBtn.setOnClickListener {
            entityDao.insert(generateEntity())
            Toast.makeText(this, "generated", Toast.LENGTH_SHORT).show()
        }

        getBtn = findViewById(R.id.get)
        getBtn.setOnClickListener{
            val received: List<Entity> = entityDao.getAll()
            val text = buildString {
                for (item in received){
                    append(item.key).append("\n")
                }
            }
            if(text.isBlank())
                Toast.makeText(this, "empty", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }

        remBtn = findViewById(R.id.wipe)
        remBtn.setOnClickListener{
            entityDao.deleteAll()
            Toast.makeText(this, "clear", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateEntity(): Entity{
        val generated = Bcrypt.hash(Random.nextInt(1,500).toString(), 12)
            .toString()
            .substring(3)
        return Entity(key = generated)
    }
}
