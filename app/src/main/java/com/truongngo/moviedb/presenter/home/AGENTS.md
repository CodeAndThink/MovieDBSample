# Home — AI rules

- Luồng: HomeFragment → HomeViewModel → LoadHomeMoviesUseCase → MovieRepository; giữ XML/View Binding, model domain và HomeEffect.
- Ba danh sách ngang chỉ hiển thị tối đa 10 phim đầu, observePreview Room LIMIT 10. Không Pager/append/scroll-trigger trên Home; refresh preview chỉ tải trang 1 qua RemoteMediator và dùng chung cache với MovieList.
- Khi preview chuyển từ rỗng sang có phim, đặt anchor về đầu hoặc khôi phục scroll riêng theo feed; không để ô > giữ anchor kéo hàng về cuối. Tắt save hierarchy mặc định của RecyclerView vì layout include dùng chung ID; giữ scroll khi Back/recreate.
- Header có See more và cuối mỗi hàng luôn có ô icon > (ConcatAdapter). Cả hai gửi SeeMoreClicked → NavigateMovieList(feed) → NavigationViewModel; không điều hướng trực tiếp bằng NavController.
- Không hiển thị Swipe for more/Load more. Footer chỉ dành cho trạng thái rỗng/lỗi và retry. Không chèn phim giả vào model để làm ô See more.
- Pull refresh tải banner và ba preview đồng thời; cache <30 phút bỏ qua tải đầu. Giữ nội dung cũ khi lỗi; job/generation chống response cũ.
- Banner Now Playing giữ cache trang 1, distinct ID, tối đa 10; sentinel loop, không timer. Lỗi cache banner không che dữ liệu mạng.
- Home và MovieList dùng chung mutex theo cache key để serialize tải mạng; cache write bị từ chối phải báo lỗi retry. Lỗi observe Room phải khởi động lại observer khi retry/refresh.
- Cache theo feed/language/region; ghi rows + remote key trong transaction. CancellationException truyền tiếp; không tự logout khi TMDB lỗi.
- onDestroyView bỏ adapter, callback/listener và binding. Không giữ View trong ViewModel.
- Test: HomeViewModelTest, HomeCacheIntegrationTest, HomeRemoteMediatorTest, HomePagingDatabaseTest, MovieListInstrumentedTest; giới hạn 10, cache chung, See more đúng feed, retry và Back.
