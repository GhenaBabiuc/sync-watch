import React from 'react';
import {Container, Navbar} from 'react-bootstrap';
import {Link} from 'react-router-dom';
import '@fortawesome/fontawesome-free/css/all.min.css';

const NavBar = ({user}) => {
    return (
        <Navbar variant="dark" className="sticky-top">
            <Container>
                <Link to="/" className="navbar-brand fs-4">
                    <i className="fas fa-play-circle me-2"></i>Sync Watch
                </Link>
                {user && user.id && (
                    <div className="d-flex align-items-center">
                        <div className="user-avatar me-2">{user.username.substring(0, 1).toUpperCase()}</div>
                        <span className="text-light me-3 fw-bold">{user.username}</span>
                        <Link to="/set-username" className="btn btn-outline-light btn-sm rounded-pill px-3">
                            <i className="fas fa-edit me-1"></i> Rename
                        </Link>
                    </div>
                )}
            </Container>
        </Navbar>
    );
};

export default NavBar;
