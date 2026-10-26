package com.truongngo.moviedb.presenter.movielist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.truongngo.moviedb.R
import com.truongngo.moviedb.data.network.utils.NetworkUtils
import com.truongngo.moviedb.databinding.ItemMovieListBinding
import com.truongngo.moviedb.domain.model.HomeMovie

class MovieListAdapter(private val onMovieClick: (Int) -> Unit) : PagingDataAdapter<HomeMovie, MovieListAdapter.Holder>(Diff) {
    class Holder(val binding: ItemMovieListBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemMovieListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val movie = getItem(position) ?: return
        with(holder.binding) {
            title.text = movie.title
            val year = movie.releaseDate?.take(4)?.takeIf { it.isNotBlank() } ?: root.context.getString(R.string.home_unknown_year)
            metadata.text = root.context.getString(R.string.home_movie_metadata, movie.voteAverage, year)
            poster.load(NetworkUtils.imageUrl(movie.posterPath, "w342")) {
                placeholder(R.drawable.home_image_placeholder)
                error(R.drawable.home_image_placeholder)
                fallback(R.drawable.home_image_placeholder)
            }
            root.setOnClickListener { onMovieClick(movie.id) }
        }
    }
    private object Diff : DiffUtil.ItemCallback<HomeMovie>() {
        override fun areItemsTheSame(oldItem: HomeMovie, newItem: HomeMovie) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HomeMovie, newItem: HomeMovie) = oldItem == newItem
    }
}
