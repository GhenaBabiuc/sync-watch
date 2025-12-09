import React, {useEffect, useState} from 'react';
import axios from 'axios';
import {useNavigate} from 'react-router-dom';
import {Alert, Badge, Button, Card, Container, Tab, Tabs} from 'react-bootstrap';
import RoomModals from '../components/RoomModals';
import {STORAGE_API_URL, SYNC_API_URL} from '../api';

const Dashboard = ({user}) => {
    const [content, setContent] = useState({movies: [], series: []});
    const [roomsData, setRoomsData] = useState({rooms: [], movieRoomCounts: {}, seriesRoomCounts: {}});
    const [loadingContent, setLoadingContent] = useState(true);
    const [error, setError] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [modalContent, setModalContent] = useState(null);
    const [modalContentType, setModalContentType] = useState(null);
    const navigate = useNavigate();
    const backgroundUrl = '/images/main-background.jpeg';

    useEffect(() => {
        const fetchContent = async () => {
            try {
                const [moviesRes, seriesRes] = await Promise.all([
                    axios.get(`${STORAGE_API_URL}/movies?size=100`),
                    axios.get(`${STORAGE_API_URL}/series?size=100`)
                ]);
                setContent({
                    movies: moviesRes.data.content || moviesRes.data || [],
                    series: seriesRes.data.content || seriesRes.data || []
                });
                setLoadingContent(false);
            } catch (error) {
                console.error("Error fetching content", error);
                setError('Failed to fetch content. Ensure Storage service is running.');
                setLoadingContent(false);
            }
        };
        fetchContent();
    }, []);

    useEffect(() => {
        const fetchRooms = async () => {
            try {
                const dashboardRes = await axios.get(`${SYNC_API_URL}/dashboard`);
                const rooms = dashboardRes.data.rooms || [];

                const movieRoomCounts = {};
                const seriesRoomCounts = {};
                rooms.forEach(room => {
                    if (room.roomType === 'MOVIE' && room.movie) {
                        const key = 'movie_' + room.movie.id;
                        movieRoomCounts[key] = (movieRoomCounts[key] || 0) + 1;
                    } else if (room.roomType === 'SERIES' && room.series) {
                        const key = 'series_' + room.series.id;
                        seriesRoomCounts[key] = (seriesRoomCounts[key] || 0) + 1;
                    }
                });
                setRoomsData({rooms, movieRoomCounts, seriesRoomCounts});
            } catch (error) {
                console.error("Error fetching rooms", error);
            }
        };
        fetchRooms();
        const intervalId = setInterval(fetchRooms, 5000);
        return () => clearInterval(intervalId);
    }, []);

    const getCoverUrl = (content, type) => {
        if (content.coverUrl && content.coverUrl.startsWith('http')) {
            return content.coverUrl;
        }
        if (type === 'MOVIE') {
            return `${STORAGE_API_URL}/stream/movies/${content.id}/cover`;
        } else if (type === 'SERIES') {
            return `${STORAGE_API_URL}/stream/series/${content.id}/cover`;
        }
        return '/images/default-cover.jpg';
    };

    const openCreateModal = (content, type) => {
        const contentWithUrl = content ? {
            ...content,
            coverUrl: getCoverUrl(content, type),
            streamUrl: type === 'MOVIE' ? `${STORAGE_API_URL}/stream/movies/${content.id}` : null
        } : null;

        setModalContent(contentWithUrl);
        setModalContentType(type);
        setShowModal(true);
    };

    const handleRoomCreated = (roomId) => {
        setShowModal(false);
        navigate(`/room/${roomId}`);
    };

    const formatDuration = (totalMinutes) => {
        if (!totalMinutes) return 'N/A';
        const h = Math.floor(totalMinutes / 60);
        const m = totalMinutes % 60;
        return h > 0 ? `${h}h ${m}m` : `${m}m`;
    };

    if (loadingContent) {
        return <div className="text-center mt-5 text-secondary">
            <i className="fas fa-spinner fa-spin fa-2x mb-3"></i>
            <p>Loading content...</p>
        </div>;
    }

    const {movies, series} = content;
    const {rooms, movieRoomCounts, seriesRoomCounts} = roomsData;

    return (
        <div style={{
            backgroundImage: `linear-gradient(rgba(15, 23, 42, 0.7), rgba(15, 23, 42, 0.9)), url(${backgroundUrl})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            backgroundAttachment: 'fixed',
            minHeight: 'calc(100vh - 56px)',
            width: '100%'
        }}>
            <Container className="py-5">
                <div className="row">
                    <div className="col-lg-8 mb-5">
                        <h2 className="mb-4 d-flex align-items-center text-white"
                            style={{textShadow: '0 2px 4px rgba(0,0,0,0.5)'}}>
                            <span className="me-2"><i className="fas fa-cubes"></i></span>
                            Available Content
                        </h2>

                        {error && <Alert variant="danger">{error}</Alert>}

                        <Tabs defaultActiveKey="movies" id="contentTabs" className="mb-4">
                            <Tab eventKey="movies"
                                 title={<><i className="fas fa-film me-2"></i>Movies ({movies.length})</>}>
                                <div className="row g-4 mt-1">
                                    {movies.length === 0 &&
                                        <Alert variant="dark" className="border-0 bg-opacity-50">No movies available
                                            yet.</Alert>}
                                    {movies.map(movie => (
                                        <div key={movie.id} className="col-md-6 col-lg-4">
                                            <Card className="content-card"
                                                  onClick={() => openCreateModal(movie, 'MOVIE')}>
                                                <div className="position-relative overflow-hidden">
                                                    <img
                                                        src={getCoverUrl(movie, 'MOVIE')}
                                                        className="content-image"
                                                        alt={movie.title}
                                                        onError={(e) => {
                                                            e.target.src = '/images/default-movie-cover.jpg';
                                                        }}
                                                    />
                                                    <Badge pill bg="dark" className="content-type-badge text-white">
                                                        <i className="fas fa-film me-1"></i> Movie
                                                    </Badge>
                                                </div>

                                                <Card.Body className="d-flex flex-column p-3">
                                                    <h6 className="card-title text-truncate mb-2 fw-bold text-white"
                                                        title={movie.title}>
                                                        {movie.title}
                                                    </h6>

                                                    <div
                                                        className="card-text small mb-4 d-flex align-items-center text-secondary">
                                                    <span
                                                        className="badge bg-dark border border-secondary text-secondary me-2">
                                                        <i className="fas fa-clock me-1"></i> {formatDuration(movie.duration)}
                                                    </span>
                                                        {movie.year && (
                                                            <span
                                                                className="badge bg-dark border border-secondary text-secondary">
                                                            {movie.year}
                                                        </span>
                                                        )}
                                                    </div>

                                                    <div
                                                        className="mt-auto pt-3 border-top border-secondary border-opacity-25 d-flex justify-content-between align-items-center">
                                                        <div className="d-flex align-items-center"
                                                             style={{fontSize: '0.85rem'}}>
                                                            <i className="fas fa-door-open text-primary me-2"></i>
                                                            <span className="fw-bold text-white me-1">
                                                            {movieRoomCounts['movie_' + movie.id] || 0}
                                                        </span>
                                                            <span className="text-secondary opacity-75">rooms</span>
                                                        </div>
                                                        <Button variant="primary" size="sm"
                                                                className="rounded-pill px-3 fw-bold shadow-sm">
                                                            <i className="fas fa-play me-1"></i> Watch
                                                        </Button>
                                                    </div>
                                                </Card.Body>
                                            </Card>
                                        </div>
                                    ))}
                                </div>
                            </Tab>
                            <Tab eventKey="series"
                                 title={<><i className="fas fa-tv me-2"></i>Series ({series.length})</>}>
                                <div className="row g-4 mt-1">
                                    {series.length === 0 &&
                                        <Alert variant="dark" className="border-0 bg-opacity-50">No series available
                                            yet.</Alert>}
                                    {series.map(s => (
                                        <div key={s.id} className="col-md-6 col-lg-4">
                                            <Card className="content-card" onClick={() => openCreateModal(s, 'SERIES')}>
                                                <div className="position-relative overflow-hidden">
                                                    <img
                                                        src={getCoverUrl(s, 'SERIES')}
                                                        className="content-image"
                                                        alt={s.title}
                                                        onError={(e) => {
                                                            e.target.src = '/images/default-series-cover.jpg';
                                                        }}
                                                    />
                                                    <Badge pill bg="success" className="content-type-badge text-white"
                                                           style={{borderColor: '#10b981'}}>
                                                        <i className="fas fa-tv me-1"></i> Series
                                                    </Badge>
                                                </div>

                                                <Card.Body className="d-flex flex-column p-3">
                                                    <h6 className="card-title text-truncate mb-2 fw-bold text-white"
                                                        title={s.title}>
                                                        {s.title}
                                                    </h6>

                                                    <div
                                                        className="card-text small mb-4 d-flex align-items-center flex-wrap gap-2">
                                                    <span
                                                        className="badge bg-dark border border-secondary text-secondary">
                                                        {s.totalSeasons || 0} seasons
                                                    </span>
                                                        <span
                                                            className="badge bg-dark border border-secondary text-secondary">
                                                        {s.totalEpisodes || 0} eps
                                                    </span>
                                                    </div>

                                                    <div
                                                        className="mt-auto pt-3 border-top border-secondary border-opacity-25 d-flex justify-content-between align-items-center">
                                                        <div className="d-flex align-items-center"
                                                             style={{fontSize: '0.85rem'}}>
                                                            <i className="fas fa-door-open text-success me-2"></i>
                                                            <span className="fw-bold text-white me-1">
                                                            {seriesRoomCounts['series_' + s.id] || 0}
                                                        </span>
                                                            <span className="text-secondary opacity-75">rooms</span>
                                                        </div>
                                                        <Button variant="success" size="sm"
                                                                className="rounded-pill px-3 fw-bold shadow-sm">
                                                            <i className="fas fa-play me-1"></i> Watch
                                                        </Button>
                                                    </div>
                                                </Card.Body>
                                            </Card>
                                        </div>
                                    ))}
                                </div>
                            </Tab>
                            <Tab eventKey="custom" title={<><i className="fas fa-link me-2"></i>Link / YouTube</>}>
                                <Card className="glass-panel text-white border border-secondary p-5 text-center"
                                      onClick={() => openCreateModal(null, 'CUSTOM')}>
                                    <i className="fab fa-youtube fa-4x mb-3 text-danger"></i>
                                    <h3 className="mb-3">Watch Web Content</h3>
                                    <p className="text-secondary mb-4" style={{maxWidth: '600px', margin: '0 auto'}}>
                                        Paste a link to a YouTube video or a direct link to a video file (.mp4, .m3u8)
                                        to
                                        watch with friends.
                                    </p>
                                    <Button variant="primary" size="lg" className="rounded-pill px-5 mx-auto"
                                            style={{maxWidth: '300px'}}>
                                        <i className="fas fa-plus me-2"></i> Create Custom Room
                                    </Button>
                                </Card>
                            </Tab>
                        </Tabs>
                    </div>

                    <div className="col-lg-4">
                        <h2 className="mb-4 d-flex align-items-center text-white"
                            style={{textShadow: '0 2px 4px rgba(0,0,0,0.5)'}}>
                            <span className="me-2"><i className="fas fa-door-open"></i></span> Active Rooms
                        </h2>

                        {rooms.length === 0 && (
                            <Alert variant="dark" className="border-0 bg-opacity-50 text-center py-4">
                                <i className="fas fa-ghost fa-2x mb-3 text-muted"></i>
                                <p className="mb-0 text-muted">No active rooms right now.<br/>Be the first to create
                                    one!
                                </p>
                            </Alert>
                        )}

                        {rooms.map(room => (
                            <Card key={room.id} className="room-card mb-3">
                                <Card.Body>
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <h6 className="card-title text-white mb-0">{room.name}</h6>
                                        <Badge bg={room.playing ? 'danger' : 'secondary'}
                                               className={room.playing ? 'animate-pulse' : ''}>
                                            <i className="fas fa-play me-1"></i> {room.playing ? 'Live' : 'Paused'}
                                        </Badge>
                                    </div>
                                    <p className="card-text mb-3">
                                        <small className="text-secondary d-block mb-2">{room.contentTitle}</small>
                                        <Badge bg="dark" className="border border-secondary text-secondary me-1">
                                            {room.roomType.toLowerCase()}
                                        </Badge>
                                        <Badge bg="dark" className="border border-secondary text-secondary">
                                            <i className="fas fa-user me-1"></i> {room.userCount}
                                        </Badge>
                                    </p>
                                    <Button variant="outline-primary" size="sm" className="w-100"
                                            onClick={() => navigate(`/room/${room.id}`)}>
                                        Join Room <i className="fas fa-arrow-right ms-1"></i>
                                    </Button>
                                </Card.Body>
                            </Card>
                        ))}
                    </div>
                </div>

                <RoomModals
                    user={user}
                    show={showModal}
                    onHide={() => setShowModal(false)}
                    content={modalContent}
                    contentType={modalContentType}
                    onRoomCreated={handleRoomCreated}
                />
            </Container>
        </div>
    );
};

export default Dashboard;
