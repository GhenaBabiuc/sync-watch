import React, {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {Button, Card, Container, Form} from 'react-bootstrap';

const SetUsername = ({user, setUser}) => {
    const [username, setUsername] = useState(user.username);
    const navigate = useNavigate();

    const handleSubmit = (e) => {
        e.preventDefault();
        if (username.trim()) {
            localStorage.setItem('username', username.trim());
            setUser({...user, username: username.trim()});
            navigate('/');
        }
    };

    return (
        <Container fluid className="vh-100 d-flex align-items-center justify-content-center">
            <Link to="/" className="navbar-brand position-absolute top-0 start-0 m-3 text-white">
                <i className="fas fa-arrow-left me-2"></i>Sync Watch
            </Link>
            <Card className="p-4" style={{
                backgroundColor: 'rgba(30, 41, 59, 0.8)',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                maxWidth: '400px'
            }}>
                <div className="text-center mb-4">
                    <div
                        className="bg-primary bg-gradient rounded-circle d-inline-flex align-items-center justify-content-center mb-3"
                        style={{width: '60px', height: '60px'}}>
                        <i className="fas fa-user-edit text-white fa-lg"></i>
                    </div>
                    <h4>Change Username</h4>
                    <p className="text-muted small">Update your display name for rooms</p>
                </div>
                <Form onSubmit={handleSubmit}>
                    <Form.Group className="mb-4">
                        <Form.Label className="text-muted small text-uppercase fw-bold">Username</Form.Label>
                        <Form.Control
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            maxLength="50"
                            placeholder="Enter your name"
                            className="bg-dark text-white border-secondary"
                        />
                    </Form.Group>
                    <div className="d-grid gap-2">
                        <Button type="submit" variant="primary" className="fw-bold">
                            Save Changes
                        </Button>
                        <Link to="/" className="btn btn-outline-secondary">
                            Cancel
                        </Link>
                    </div>
                </Form>
            </Card>
        </Container>
    );
};

export default SetUsername;
