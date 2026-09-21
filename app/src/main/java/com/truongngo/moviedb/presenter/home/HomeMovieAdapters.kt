package com.truongngo.moviedb.presenter.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.truongngo.moviedb.R
import com.truongngo.moviedb.domain.model.HomeMovie
import com.truongngo.moviedb.data.network.utils.NetworkUtils
import com.truongngo.moviedb.databinding.ItemHomeBannerBinding
import com.truongngo.moviedb.databinding.ItemHomeMovieBinding

class HomeMovieAdapter(private val onMovieClick: (HomeMovie) -> Unit = {}) : ListAdapter<HomeMovie, HomeMovieAdapter.Holder>(MovieDiff) {
    class Holder(val binding: ItemHomeMovieBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemHomeMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val movie = getItem(position)
        holder.binding.apply {
            root.setOnClickListener { onMovieClick(movie) }
            root.isFocusable = true
            title.text = movie.title
            metadata.bindMetadata(movie)
            poster.load(NetworkUtils.imageUrl(movie.posterPath, "w342")) {
                crossfade(true)
                placeholder(R.drawable.home_image_placeholder)
                error(R.drawable.home_image_placeholder)
                fallback(R.drawable.home_image_placeholder)
            }
        }
    }

    private object MovieDiff : DiffUtil.ItemCallback<HomeMovie>() {
        override fun areItemsTheSame(oldItem: HomeMovie, newItem: HomeMovie) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HomeMovie, newItem: HomeMovie) = oldItem == newItem
    }
}

/** Two sentinel pages allow seamless wrapping without allocating an unbounded adapter. */
internal object BannerPages {
    fun count(size: Int) = if (size > 1) size + 2 else size
    fun movieIndex(position: Int, size: Int) = if (size <= 1) 0 else Math.floorMod(position - 1, size)
    fun settledPosition(position: Int, size: Int): Int = when {
        size <= 1 -> 0
        position == 0 -> size
        position == size + 1 -> 1
        else -> position
    }
}

class HomeBannerAdapter(private val onMovieClick: (HomeMovie) -> Unit = {}) : RecyclerView.Adapter<HomeBannerAdapter.Holder>() {
    var movies: List<HomeMovie> = emptyList()
        private set

    class Holder(val binding: ItemHomeBannerBinding) : RecyclerView.ViewHolder(binding.root)

    fun submitMovies(value: List<HomeMovie>): Boolean {
        val next = value.take(10)
        if (movies == next) return false
        movies = next
        notifyDataSetChanged()
        return true
    }

    override fun getItemCount() = BannerPages.count(movies.size)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemHomeBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val movie = movies[BannerPages.movieIndex(position, movies.size)]
        holder.binding.apply {
            root.setOnClickListener { onMovieClick(movie) }
            root.isFocusable = true
            title.text = movie.title
            metadata.bindMetadata(movie)
            backdrop.load(NetworkUtils.imageUrl(movie.backdropPath ?: movie.posterPath, "w780")) {
                crossfade(true)
                placeholder(R.drawable.home_image_placeholder)
                error(R.drawable.home_image_placeholder)
                fallback(R.drawable.home_image_placeholder)
            }
        }
    }
}

private fun TextView.bindMetadata(movie: HomeMovie) {
    val year = movie.releaseDate?.take(4)?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.home_unknown_year)
    text = context.getString(R.string.home_movie_metadata, movie.voteAverage, year)
}
