package com.bzh.dytt.ui.home

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.recyclerview.extensions.AsyncDifferConfig
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bzh.dytt.AppExecutors
import com.bzh.dytt.R
import com.bzh.dytt.base.BaseFragment
import com.bzh.dytt.di.GlideApp
import com.bzh.dytt.vo.ExceptionType
import com.bzh.dytt.vo.MovieDetail
import com.bzh.dytt.vo.Resource
import com.bzh.dytt.vo.Status
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.error_layout.*
import kotlinx.android.synthetic.main.item_home_child.view.*
import kotlinx.android.synthetic.main.single_list_page.*
import javax.inject.Inject

class HomeListFragment : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appExecutors: AppExecutors

    private var otherExceptionObserver: Observer<Resource<ExceptionType>> = Observer { result ->
        when (result?.data) {
            ExceptionType.TaskFailure -> {
                if (activity != null) {
                    Toast.makeText(activity, resources.getString(R.string.fetch_video_detail_exception, result.message), Toast.LENGTH_SHORT).show()
                } else {
                    Logger.e(TAG, "onChanged: activity is null")
                }
            }
        }
    }

    private var refreshListener: SwipeRefreshLayout.OnRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        listViewModel.refresh()
    }

    private var listObserver: Observer<Resource<List<MovieDetail>>> = Observer { result ->

        when (result?.status) {
            Status.ERROR -> {
                swipe_refresh_layout.isRefreshing = false
                error_layout.visibility = View.VISIBLE
                empty_layout.visibility = View.GONE

            }
            Status.LOADING -> {
                swipe_refresh_layout.isRefreshing = true
                error_layout.visibility = View.GONE
                empty_layout.visibility = View.GONE
            }
            Status.SUCCESS -> {
                swipe_refresh_layout.isRefreshing = false
                if (result.data == null || result.data.isEmpty()) {
                    empty_layout.visibility = View.VISIBLE
                    error_layout.visibility = View.GONE
                } else {
                    empty_layout.visibility = View.GONE
                    error_layout.visibility = View.GONE
                    homeListAdapter.submitList(result.data)
                }
            }
        }
    }

    lateinit var listViewModel: HomeListViewModel

    lateinit var homeListAdapter: HomeListAdapter

    override fun doCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.single_list_page, container, false)
    }

    override fun doViewCreated(view: View, savedInstanceState: Bundle?) {
        super.doViewCreated(view, savedInstanceState)

        val movieType = arguments?.getSerializable(MOVIE_TYPE)
        listViewModel = viewModelFactory.create(HomeListViewModel::class.java)
        listViewModel.moveTypeLiveData.value = movieType as HomeViewModel.HomeMovieType
        swipe_refresh_layout.setOnRefreshListener(refreshListener)
        listview.layoutManager = LinearLayoutManager(activity)
        homeListAdapter = HomeListAdapter(appExecutors)
        listview.adapter = homeListAdapter

        lifecycle.addObserver(listViewModel)
        listViewModel.movieListLiveData.observe(this, listObserver)
    }

    override fun doDestroyView() {
        listViewModel.movieListLiveData.removeObserver(listObserver)
        lifecycle.removeObserver(listViewModel)
        super.doDestroyView()
    }

    class HomeListAdapter constructor(appExecutors: AppExecutors) : ListAdapter<MovieDetail, MovieItemHolder>(
            AsyncDifferConfig
                    .Builder<MovieDetail>(object : DiffUtil.ItemCallback<MovieDetail>() {
                        override fun areItemsTheSame(oldItem: MovieDetail, newItem: MovieDetail): Boolean {
                            return oldItem.id == newItem.id
                                    && oldItem.categoryId == newItem.categoryId
                        }

                        override fun areContentsTheSame(oldItem: MovieDetail, newItem: MovieDetail): Boolean {
                            return oldItem.name == newItem.name
                                    && oldItem.homePicUrl == newItem.homePicUrl
                        }
                    })
                    .setBackgroundThreadExecutor(appExecutors.diskIO())
                    .build()
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieItemHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_child, parent, false)
            return MovieItemHolder(view)
        }

        override fun onBindViewHolder(holder: MovieItemHolder, position: Int) {

            val item = getItem(position)

            // clear value
            holder.itemView.video_title.text = ""
            holder.itemView.video_publish_time.text = ""
            holder.itemView.douban_grade.text = ""
            holder.itemView.imdb_grade.text = ""
            holder.itemView.video_description.text = ""
            holder.itemView.setOnClickListener(null)
            GlideApp.with(holder.itemView.context)
                    .load("")
                    .placeholder(R.drawable.default_video)
                    .into(holder.itemView.video_cover)

            // update value
            holder.itemView.video_title.text = item.name
            holder.itemView.video_publish_time.text = item.publishTime
            holder.itemView.video_description.text = item.content
            if (!TextUtils.isEmpty(item.homePicUrl)) {
                GlideApp.with(holder.itemView.context)
                        .load(item.homePicUrl)
                        .placeholder(R.drawable.default_video)
                        .into(holder.itemView.video_cover)
            }

            holder.itemView.setOnClickListener {
            }
        }
    }

    class MovieItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {

        private const val TAG = "HomeListFragment"

        private const val MOVIE_TYPE = "MOVIE_TYPE"

        fun newInstnace(movieType: HomeViewModel.HomeMovieType): HomeListFragment {
            val args = Bundle()
            args.putSerializable(MOVIE_TYPE, movieType)
            val fragment = HomeListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}