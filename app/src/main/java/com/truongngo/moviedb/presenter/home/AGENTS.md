# Home — AI rules

- Luồng: HomeFragment → HomeEvent → HomeViewModel → ApiClients; HomeState/HomeEffect.
- Banner Now Playing: trang 1, distinct ID, tối đa 10; sentinel loop, không timer. Ba danh sách ngang có state phân trang riêng.
- Refresh bốn API đồng thời; giữ nội dung cũ khi chờ/lỗi. Giữ generation check chống pagination cũ ghi đè refresh.
- Load-more nối distinct ID, tối đa min(totalPages, 500); retry đúng trang lỗi. Không load-more trong refresh/loading.
- Test: HomeViewModelTest, HomeInstrumentedTest; chú ý refresh race, retry, banner wrap.
