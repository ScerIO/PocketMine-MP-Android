package io.scer.pocketmine.screens

import android.Manifest
import android.os.Bundle
import androidx.annotation.Nullable
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage
import io.scer.pocketmine.R
import android.content.Intent
import androidx.fragment.app.Fragment


class IntroActivity : AppIntro() {
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStatusBar(false)

        val sliderPage = SliderPage()
        sliderPage.title = resources.getString(R.string.intro_1_title)
        sliderPage.description = resources.getString(R.string.intro_1_description)
        sliderPage.imageDrawable = R.drawable.ic_cloud_white_192
        sliderPage.bgColor = resources.getColor(R.color.introOne)
        addSlide(AppIntroFragment.newInstance(sliderPage))

        val downloadsPage = SliderPage()
        downloadsPage.title = resources.getString(R.string.intro_2_title)
        downloadsPage.description = resources.getString(R.string.intro_2_description)
        downloadsPage.imageDrawable = R.drawable.ic_file_download_white_192
        downloadsPage.bgColor = resources.getColor(R.color.introTwo)
        addSlide(AppIntroFragment.newInstance(downloadsPage))

        val testingPage = SliderPage()
        testingPage.title = resources.getString(R.string.intro_3_title)
        testingPage.description = resources.getString(R.string.intro_3_description)
        testingPage.imageDrawable = R.drawable.ic_code_white_192
        testingPage.bgColor = resources.getColor(R.color.introThree)
        addSlide(AppIntroFragment.newInstance(testingPage))

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