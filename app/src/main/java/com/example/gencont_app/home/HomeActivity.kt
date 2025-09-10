package com.example.gencont_app.home

import com.example.gencont_app.R
import com.example.gencont_app.cours.CoursActivity
import com.example.gencont_app.formulaire.FormulaireActivity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        val btnCustomizeCourse = findViewById<MaterialButton>(R.id.btn_customize_course)
        val btnAllCourses = findViewById<MaterialButton>(R.id.btn_all_courses)
        val greetingText = findViewById<TextView>(R.id.greeting_text)

        // Set personalized greeting (optional)
        setPersonalizedGreeting(greetingText)

        // Set click listeners for the buttons
        btnCustomizeCourse.setOnClickListener {
            // Open FormulaireActivity when "Customize Your Course" button is clicked
            val intent = Intent(this@HomeActivity, FormulaireActivity::class.java)
            startActivity(intent)
        }

        btnAllCourses.setOnClickListener {
            // Open CoursActivity when "View All Courses" button is clicked
            val intent = Intent(this@HomeActivity, CoursActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Optional method to set a personalized greeting based on user data
     * You can remove this if not needed
     */
    private fun setPersonalizedGreeting(textView: TextView) {
        // This is where you would get the user's name from preferences or database
        // For example:
        // val userName = PreferenceManager.getDefaultSharedPreferences(this).getString("user_name", "")

        // For now, we'll just use the default greeting
        // If you have user data, you could do something like:
        // if (userName.isNotEmpty()) {
        //     textView.text = "Welcome back, $userName!"
        // }
    }
}