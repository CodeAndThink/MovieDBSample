# Home — AI rules

- Luồng: HomeFragment → HomeEvent → HomeViewModel → LoadHomeMoviesUseCase → MovieRepository → ApiClients + HomeCache (Room); HomeState/HomeEffect.
- Banner Now Playing: trang 1, distinct ID, tối đa 10; sentinel loop, không timer. Ba danh sách ngang có state phân trang riêng.
- Refresh bốn API đồng thời; giữ nội dung cũ khi chờ/lỗi. Giữ generation check chống pagination cũ ghi đè refresh.
- ViewModel/state dùng model domain, không import API/cache/model/exception từ data. Repository chọn nguồn dữ liệu và dùng NetworkErrorMapper chuyển lỗi sang MovieLoadError; UI dùng mapper message chung ở presenter/common; ViewModel giữ job/generation và state UI.
- Room chỉ lưu trang 1 theo feed/language/region, thay metadata + danh sách trong transaction. Khởi tạo hiển thị cache trước; cache <30 phút bỏ qua API, cache cũ giữ khi lỗi. Refresh/retry ép gọi API; lỗi cache không che dữ liệu mạng, CancellationException phải truyền tiếp.
- Load-more nối distinct ID, tối đa min(totalPages, 500); retry đúng trang lỗi. Không load-more trong refresh/loading.
- Test: HomeViewModelTest, HomeCacheIntegrationTest, MovieRepositoryImplTest, LoadHomeMoviesUseCaseTest, HomeCacheDatabaseTest, HomeInstrumentedTest; chú ý refresh race, retry, banner wrap.
