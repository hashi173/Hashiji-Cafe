/**
 * Simple Client-side Table Pagination
 * Usage: paginateTable('tableId', 10);
 */
function paginateTable(tableId, rowsPerPage) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));

    // Filter out "No data" rows if any
    const dataRows = rows.filter(r => !r.querySelector('td[colspan]'));

    if (dataRows.length <= rowsPerPage) return; // No need to paginate

    const pageCount = Math.ceil(dataRows.length / rowsPerPage);
    let currentPage = 1;

    // Create Pagination Controls
    const wrapper = document.createElement('div');
    wrapper.className = 'd-flex justify-content-center align-items-center mt-3 gap-2';

    const btnPrev = document.createElement('button');
    btnPrev.className = 'btn btn-sm btn-outline-secondary';
    btnPrev.innerHTML = '<i class="fas fa-chevron-left"></i>';
    btnPrev.onclick = () => showPage(currentPage - 1);

    const btnNext = document.createElement('button');
    btnNext.className = 'btn btn-sm btn-outline-secondary';
    btnNext.innerHTML = '<i class="fas fa-chevron-right"></i>';
    btnNext.onclick = () => showPage(currentPage + 1);

    const info = document.createElement('span');
    info.className = 'text-muted small fw-bold';

    wrapper.appendChild(btnPrev);
    wrapper.appendChild(info);
    wrapper.appendChild(btnNext);

    table.parentNode.insertBefore(wrapper, table.nextSibling);

    function showPage(page) {
        if (page < 1 || page > pageCount) return;
        currentPage = page;

        const start = (currentPage - 1) * rowsPerPage;
        const end = start + rowsPerPage;

        // Hide all data rows
        dataRows.forEach(r => r.style.display = 'none');

        // Show current page rows
        dataRows.slice(start, end).forEach(r => r.style.display = '');

        // Update Buttons
        btnPrev.disabled = currentPage === 1;
        btnNext.disabled = currentPage === pageCount;
        info.innerText = `Page ${currentPage} of ${pageCount}`;
    }

    // Init
    showPage(1);
}
