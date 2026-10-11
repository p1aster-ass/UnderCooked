package com.example.undercooked.activities; // הקובץ שייך לחבילה של האקטיביטי במשחק

import android.os.Bundle; // Bundle שומר מידע כשהמסך נסגר ונפתח מחדש

import androidx.appcompat.app.AppCompatActivity; // מחלקת בסיס לכל מסך באנדרואיד

import com.example.undercooked.R; // גישה לקבצי העיצוב של האפליקציה (layout, drawable וכו')

/**
 * GameActivity — המסך שמציג את המשחק עצמו.
 * האקטיביטי הזה טוען את ה-layout של המשחק ומציג אותו על המסך.
 */
public class GameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // onCreate נקרא כשהמסך נוצר לראשונה
        super.onCreate(savedInstanceState); // קורא לאותה פונקציה של המחלקה האם כדי שאנדרואיד יאתחל את כל הדברים הפנימיים

        setContentView(R.layout.activity_game); // אומר לאנדרואיד איזה עיצוב XML להציג על המסך הזה
    }
}