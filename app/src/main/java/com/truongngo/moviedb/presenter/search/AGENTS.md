# Search — AI rules

- Luồng: SearchFragment → SearchEvent → SearchViewModel → ApiClients; SearchState/SearchEffect.
- Debounce 500 ms; query blank không gọi API; gửi query.trim(). Giữ cancel + generation chống response cũ.
- Query mới reset danh sách; pagination distinct ID, tối đa min(totalPages, 500). Lỗi giữ trang cũ; retry page + 1.
- SavedStateHandle chỉ lưu query, không lưu kết quả. Sau process death phải tải lại.
- Root destination; Detail phải giữ Search bên dưới để Back về query/kết quả hiện tại.
- Test: SearchViewModelTest; debounce, stale response, retry, query restore.
