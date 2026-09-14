package com.truongngo.moviedb.presenter.extension

import com.truongngo.moviedb.presenter.enum.LoadStatus

val LoadStatus.isInitial : Boolean get() = this == LoadStatus.INITIAL

val LoadStatus.isLoading : Boolean get() = this == LoadStatus.LOADING

val LoadStatus.isSuccess : Boolean get() = this == LoadStatus.SUCCESS

val LoadStatus.isFailure : Boolean get() = this == LoadStatus.FAILURE