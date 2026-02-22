@file:Suppress("DEPRECATION")
package github.daisukikaffuchino.rebootnya

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.SearchView
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment
import github.daisukikaffuchino.rebootnya.databinding.ActivityLicenseBinding

class LicenseActivity : BaseActivity(){

    lateinit var fragment: LibsSupportFragment
    lateinit var binding: ActivityLicenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.licenseToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fragment = LibsSupportFragment()

        supportFragmentManager.beginTransaction().replace(R.id.license_fragment_container, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setBackgroundResource(R.drawable.search_bar_bg)
        searchView.queryHint = getString(R.string.enter_content)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                fragment.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                fragment.filter.filter(newText)
                return true
            }
        })

        return true
    }
}