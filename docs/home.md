# Home

Home uses XML/ViewBinding, a ViewPager2 banner and three horizontal RecyclerViews.
There is no two-column grid.

- Now Playing uses page 1 of `getNowPlayingMovies`, deduplicates IDs and displays
  at most 10 films. The banner uses first/last sentinel pages and resets after the
  swipe settles, so it loops in both directions without a huge adapter or timer.
- Popular, Top Rated and Upcoming each maintain their own page, loading state,
  error and end-of-list flag. Scrolling within four items of the end loads the
  next page. The Load more button also supports tapping/accessibility.
- Pulling down at the top refreshes all four APIs concurrently. Existing content
  stays visible during refresh and on failure. Successful refreshes replace old
  lists; pagination appends unique movie IDs. A generation check prevents a late
  pagination result from overwriting refreshed content.
- Errors appear inline with Try again. Failed pagination retries the same page;
  a failed refresh retries page 1. No Firebase logout is triggered by TMDB errors.
- Coil loads posters/backdrops with cache, crossfade and fallback images. Its
  image client does not receive the TMDB API credentials.

Main files are in `app/src/main/java/com/truongngo/moviedb/presenter/home` and
`app/src/main/res/layout/{fragment_home,layout_home_section,item_home_movie,item_home_banner}.xml`.

Validation:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.truongngo.moviedb.HomeInstrumentedTest
```

Unit tests cover pagination, deduplication, errors/retries, refresh races and
banner position mapping. The emulator smoke test uses the configured service,
checks rendering, banner wrap when movies are available, pull-to-refresh and
Home view recreation. It accepts an inline error if the service is unavailable.
