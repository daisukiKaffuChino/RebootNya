@file:Suppress("DEPRECATION")

package github.daisukikaffuchino.rebootnya.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment
import github.daisukikaffuchino.rebootnya.R

class LicenseFragment : Fragment() {

    private var isCollapsing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_license, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val libsFragment = childFragmentManager.findFragmentByTag(LIBS_FRAGMENT_TAG) as? LibsSupportFragment
            ?: LibsSupportFragment().also {
                childFragmentManager.beginTransaction()
                    .replace(R.id.license_fragment_container, it, LIBS_FRAGMENT_TAG)
                    .commit()
            }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_search, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                searchView.setBackgroundResource(R.drawable.search_bar_bg)
                searchView.queryHint = getString(R.string.enter_content)
                searchView.alpha = 0f

                searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        searchView.scaleX = 0.95f
                        searchView.scaleY = 0.95f
                        searchView.alpha = 0f

                        searchView.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        if (isCollapsing) {
                            isCollapsing = false
                            return true
                        }

                        searchView.animate()
                            .alpha(0f)
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(150)
                            .withEndAction {
                                isCollapsing = true
                                item.collapseActionView()
                            }
                            .start()

                        return false
                    }
                })

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        libsFragment.filter.filter(query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        libsFragment.filter.filter(newText)
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    companion object {
        private const val LIBS_FRAGMENT_TAG = "libs_fragment"
    }
}
