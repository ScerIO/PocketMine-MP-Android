package io.scer.pocketmine.screens

import android.os.Bundle
import androidx.annotation.Nullable
import com.github.paolorotolo.appintro.AppIntroFragment
import io.scer.pocketmine.R
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro2
import io.scer.pocketmine.screens.home.MainActivity

class IntroActivity : AppIntro2() {
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setColorTransitionsEnabled(true)

        addSlide(AppIntroFragment.newInstance(
                title = resources.getString(R.string.intro_1_title),
                description = resources.getString(R.string.intro_1_description),
                imageDrawable = R.drawable.ic_cloud_white_192,
                bgColor = ContextCompat.getColor(this, R.color.introOne)
        ))

        addSlide(AppIntroFragment.newInstance(
                title = resources.getString(R.string.intro_2_title),
                description = resources.getString(R.string.intro_2_description),
                imageDrawable = R.drawable.ic_file_download_white_192,
                bgColor = ContextCompat.getColor(this, R.color.introTwo)
        ))

        addSlide(AppIntroFragment.newInstance(
                title = resources.getString(R.string.intro_3_title),
                description = resources.getString(R.string.intro_3_description),
                imageDrawable = R.drawable.ic_code_white_192,
                bgColor = ContextCompat.getColor(this, R.color.introThree)
        ))
    }

    override fun onSkipPressed(currentFragment: Fragment) {
        super.onSkipPressed(currentFragment)
        done()
    }

    override fun onDonePressed(currentFragment: Fragment) {
        super.onDonePressed(currentFragment)
        done()
    }

    private fun done() {
        val preferences = getSharedPreferences("launcher", 0).edit()
        preferences.putBoolean("initialized", true)
        preferences.apply()
        replaceActivity(MainActivity::class.java)
    }

    private fun <T> replaceActivity(activity: Class<T>) {
        val intent = Intent(this, activity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(intent)
        this.finish()
    }
}