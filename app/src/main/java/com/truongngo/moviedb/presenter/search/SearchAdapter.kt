package com.truongngo.moviedb.presenter.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.truongngo.moviedb.R
import com.truongngo.moviedb.data.network.model.Movie
import com.truongngo.moviedb.data.network.utils.NetworkUtils
import com.truongngo.moviedb.databinding.ItemSearchMovieBinding

class SearchAdapter(private val onClick: (Int) -> Unit) : ListAdapter<Movie, SearchAdapter.Holder>(Diff) {
    class Holder(val binding: ItemSearchMovieBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemSearchMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        val movie = getItem(position)
        title.text = movie.title
        metadata.text = root.context.getString(R.string.home_movie_metadata, movie.voteAverage,
            movie.releaseDate?.take(4).orEmpty())
        overview.text = movie.overview
        thumbnail.load(NetworkUtils.imageUrl(movie.backdropPath ?: movie.posterPath, "w780")) {
            crossfade(true)
            placeholder(R.drawable.home_image_placeholder)
            error(R.drawable.home_image_placeholder)
            fallback(R.drawable.home_image_placeholder)
        }
        root.setOnClickListener { onClick(movie.id) }
    }
    private object Diff : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie) = oldItem == newItem
    }
}
