# Movie list — AI rules

- Root destination MovieList(feed), yêu cầu phiên; feed chỉ POPULAR/TOP_RATED/UPCOMING. SavedStateHandle lưu feed; không lưu danh sách.
- MovieListViewModel → LoadHomeMoviesUseCase.paged → MovieRepository → Room + RemoteMediator; cachedIn(viewModelScope). Dùng cache chung với preview Home.
- Danh sách dọc phân trang, refresh/retry qua PagingDataAdapter. Giữ nội dung khi lỗi, hiển thị lỗi append và refresh; không tự logout khi TMDB lỗi.
- Event, Effect, ViewModel, Fragment và Adapter đặt trong file riêng như Home.
- Event/effect cho Back và Detail, điều hướng NavigationViewModel → AppNavigator. Back từ Detail giữ danh sách bên dưới.
- collectLatest/repeatOnLifecycle theo view; giải phóng adapter/listener/binding khi destroy view.
- Test: MovieListViewModelTest, DeepLinkServiceTest, NavigationViewModelTest, MovieListInstrumentedTest; kiểm tra feed, pending login, Back và refresh/offline.
