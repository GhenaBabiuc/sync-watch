import React from 'react';
import {Pagination} from 'react-bootstrap';

const PaginationControl = ({currentPage, totalPages, onPageChange}) => {
    if (totalPages <= 1) return null;

    const active = currentPage + 1;
    let items = [];

    items.push(
        <Pagination.Prev
            key="prev"
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
        />
    );

    for (let number = 1; number <= totalPages; number++) {
        if (
            number === 1 ||
            number === totalPages ||
            (number >= active - 2 && number <= active + 2)
        ) {
            items.push(
                <Pagination.Item
                    key={number}
                    active={number === active}
                    onClick={() => onPageChange(number - 1)}
                >
                    {number}
                </Pagination.Item>
            );
        } else if (
            (number === active - 3 && active > 4) ||
            (number === active + 3 && active < totalPages - 3)
        ) {
            items.push(<Pagination.Ellipsis key={`ellipsis-${number}`}/>);
        }
    }

    items.push(
        <Pagination.Next
            key="next"
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage === totalPages - 1}
        />
    );

    return (
        <div className="d-flex justify-content-center mt-4">
            <Pagination>{items}</Pagination>
        </div>
    );
};

export default PaginationControl;
