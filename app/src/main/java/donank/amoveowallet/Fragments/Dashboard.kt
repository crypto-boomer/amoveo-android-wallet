package donank.amoveowallet.Fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.ObservableArrayList
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.nitrico.lastadapter.LastAdapter
import donank.amoveowallet.BR
import donank.amoveowallet.Utility.showFragment
import donank.amoveowallet.Utility.showInSnack
import donank.amoveowallet.Dagger.MainApplication
import donank.amoveowallet.Data.Model.ViewModels.SelectedWalletViewModel
import donank.amoveowallet.R
import kotlinx.android.synthetic.main.fragment_dashboard.*
import donank.amoveowallet.Data.Model.*
import donank.amoveowallet.Data.Model.ViewModels.WalletListViewModel
import donank.amoveowallet.Data.Model.ViewModels.WalletListViewModelFactory
import donank.amoveowallet.databinding.ItemWalletBinding
import kotlinx.android.synthetic.main.item_wallet.view.*
import javax.inject.Inject

class Dashboard : Fragment() {

    private val TAG = Dashboard::class.simpleName

    private val wallets = ObservableArrayList<WalletModel>()
    private val lastAdapter: LastAdapter by lazy { initLastAdapter() }

    //https://medium.com/@cdmunoz/offline-first-android-app-with-mvvm-dagger2-rxjava-livedata-and-room-part-4-2b476142e769
    @Inject
    lateinit var walletListViewModelFactory: WalletListViewModelFactory
    lateinit var walletListViewModel : WalletListViewModel
    lateinit var selectedWalletViewModel: SelectedWalletViewModel

    fun initLastAdapter(): LastAdapter {
        return LastAdapter(wallets, BR.item)
                .map<WalletModel, ItemWalletBinding>(R.layout.item_wallet) {
                    onBind {
                        it.itemView.setOnClickListener { _ ->
                            selectedWalletViewModel.select(it.binding.item!!)
                            showFragment(
                                    Fragment.instantiate(
                                            activity,
                                            Wallet::class.java.name
                                    ),
                                    addToBackStack = true
                            )
                        }
                        val addrLen = it.binding.item!!.address.length
                        val first5 = it.binding.item!!.address.substring(0, 5)
                        val last5 = it.binding.item!!.address.substring(addrLen - 6, addrLen)
                        it.itemView.t_address.text = first5.plus("...").plus(last5)
                    }
                }
                .into(wallet_recycler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as MainApplication).component.inject(this)

        walletListViewModel = ViewModelProviders.of(activity!!, walletListViewModelFactory).get(WalletListViewModel::class.java)

        walletListViewModel.loadWallets()

        walletListViewModel.walletsResult().observe(this@Dashboard, Observer<List<WalletModel>>{
            wallets.clear()
            wallets.addAll(it!!)
            lastAdapter.notifyDataSetChanged()
        })

        walletListViewModel.walletsError().observe(this@Dashboard,Observer<String>{
            showInSnack(this.view!!,"Error while retrieving wallets from db")
        })

        selectedWalletViewModel = ViewModelProviders.of(activity!!).get(SelectedWalletViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        wallet_recycler.layoutManager = LinearLayoutManager(activity,LinearLayoutManager.HORIZONTAL,false)
        wallet_recycler.adapter = lastAdapter

        bottom_navigation.setOnNavigationItemSelectedListener(
                { item ->
                    when (item.itemId) {
                        R.id.action_generate->showFragment(
                                Fragment.instantiate(
                                        activity,
                                        GenerateWallet::class.java.name
                                ),
                                addToBackStack = true
                        )
                        R.id.action_import->showFragment(
                                Fragment.instantiate(
                                        activity,
                                        ImportWallet::class.java.name
                                ),
                                addToBackStack = true
                        )
                        R.id.action_watch->showFragment(
                                Fragment.instantiate(
                                        activity,
                                        WatchWallet::class.java.name
                                ),
                                addToBackStack = true
                        )
                    }
                    true
                })

    }

    private fun showFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        fragment.showFragment(container = R.id.fragment_container,
                fragmentManager = activity!!.supportFragmentManager,
                addToBackStack = addToBackStack)
    }

    override fun onDestroy() {
        walletListViewModel.disposeElements()
        super.onDestroy()
    }
}