import React, { createContext, useContext } from 'react';

const DataTableContext = createContext({});

export default function DataTable({
  children,
  sort,
  page = 0,
  size = 20,
  onSortChange
}) {
  return (
    <DataTableContext.Provider value={{ sort, page, size, onSortChange }}>
      <div className="data-table">
        {children}
      </div>
    </DataTableContext.Provider>
  );
}

DataTable.Header = function Header({ columns }) {
  const { sort, onSortChange } = useContext(DataTableContext);

  return (
    <div className="data-table__header" role="row">
      {columns.map((column) => (
        <button
          key={column.key}
          className={sort === column.key ? 'active' : ''}
          onClick={() => onSortChange(column.key)}
        >
          {column.label}
        </button>
      ))}
    </div>
  );
};

DataTable.Body = function Body({ rows, render }) {
  return (
    <div className="data-table__body">
      {rows.map((row) => (
        <div key={row.id} className="data-table__row">
          {render(row)}
        </div>
      ))}
    </div>
  );
};

DataTable.Pagination = function Pagination({
  page,
  totalPages,
  onChange
}) {
  return (
    <nav className="data-table__pagination" aria-label="Pagination">
      <button
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        Prev
      </button>

      <span>
        {page + 1} / {totalPages}
      </span>

      <button
        disabled={page === totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </nav>
  );
};