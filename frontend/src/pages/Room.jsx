import React, {useEffect, useRef, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import axios from 'axios';
import SockJS from 'sockjs-client';
import {Client} from '@stomp/stompjs';
import {Alert, Badge, Button, Card, Col, Container, ListGroup, Row, Spinner} from 'react-bootstrap';
import SyncPlayer from '../components/SyncPlayer';
import {STORAGE_API_URL, SYNC_API_URL, WS_URL} from '../api';

const formatTime = (seconds) => {
    if (seconds === null || seconds === undefined) return '0:00';
    const totalSeconds = Math.floor(seconds);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;
    return hours > 0
        ? `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
        : `${minutes}:${secs.toString().padStart(2, '0')}`;
};

const Room = ({user}) => {
    const {roomId} = useParams();
    const navigate = useNavigate();
    const stompClientRef = useRef(null);
    const playerRef = useRef(null);
    const [room, setRoom] = useState(null);
    const [users, setUsers] = useState([]);
    const [isHost, setIsHost] = useState(false);
    const [streamUrl, setStreamUrl] = useState(null);
    const [coverUrl, setCoverUrl] = useState(null);
    const [backdropUrl, setBackdropUrl] = useState(null);
    const [currentEpisode, setCurrentEpisode] = useState(null);
    const [availableSeasons, setAvailableSeasons] = useState([]);
    const [episodesBySeason, setEpisodesBySeason] = useState({});
    const [currentSeasonId, setCurrentSeasonId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let client;

        const fetchInitialData = async () => {
            try {
                const roomRes = await axios.get(`${SYNC_API_URL}/rooms/${roomId}`);
                const initialRoom = roomRes.data;
                setRoom(initialRoom);

                const hostRes = await axios.get(`${SYNC_API_URL}/user/${user.id}/isHost/${roomId}`);
                setIsHost(hostRes.data);

                if (initialRoom.roomType === 'MOVIE') {
                    setStreamUrl(`${STORAGE_API_URL}/stream/movies/${initialRoom.contentId}`);
                    setCoverUrl(`${STORAGE_API_URL}/stream/movies/${initialRoom.contentId}/cover`);
                    setBackdropUrl(`${STORAGE_API_URL}/stream/movies/${initialRoom.contentId}/backdrop`);

                } else if (initialRoom.roomType === 'SERIES') {
                    const seriesId = initialRoom.contentId;
                    setCoverUrl(`${STORAGE_API_URL}/stream/series/${seriesId}/cover`);
                    setBackdropUrl(`${STORAGE_API_URL}/stream/series/${seriesId}/backdrop`);

                    const seasonsRes = await axios.get(`${STORAGE_API_URL}/series/${seriesId}/seasons?size=100`);
                    const seasons = seasonsRes.data.content || seasonsRes.data || [];
                    seasons.sort((a, b) => a.seasonNumber - b.seasonNumber);
                    setAvailableSeasons(seasons);

                    const seasonId = initialRoom.currentSeasonId || (seasons.length > 0 ? seasons[0].id : null);
                    const episodeId = initialRoom.currentEpisodeId;

                    if (seasonId) {
                        setCurrentSeasonId(seasonId);
                        const epList = await loadEpisodesForSeason(seasonId);

                        if (episodeId && epList) {
                            const foundEpisode = epList.find(e => e.id === episodeId);
                            if (foundEpisode) {
                                setCurrentEpisode(foundEpisode);
                                setStreamUrl(`${STORAGE_API_URL}/stream/episodes/${foundEpisode.id}`);
                            }
                        }
                    }

                } else if (initialRoom.roomType === 'CUSTOM') {
                    setStreamUrl(initialRoom.customUrl);
                }

                setLoading(false);
            } catch (err) {
                console.error("Error fetching room data", err);
                setError("Room not found or connection failed.");
                setLoading(false);
            }
        };

        fetchInitialData();

        const setupWebSocket = () => {
            if (error || loading) return;
            const socket = new SockJS(WS_URL);
            client = new Client({
                webSocketFactory: () => socket,
                onConnect: () => {
                    console.log('Connected to WS');
                    client.subscribe(`/topic/room/${roomId}/sync`, (message) => handleSyncMessage(JSON.parse(message.body)));
                    client.subscribe(`/topic/room/${roomId}/users`, (message) => setUsers(JSON.parse(message.body)));
                    client.subscribe(`/topic/room/${roomId}/episodeChanged`, (message) => handleEpisodeChange(JSON.parse(message.body)));
                    client.subscribe(`/user/queue/room/${roomId}/state`, (message) => handleInitialStateSync(JSON.parse(message.body)));

                    client.publish({
                        destination: `/app/room/${roomId}/join`,
                        body: JSON.stringify({userId: user.id, username: user.username})
                    });
                },
                onDisconnect: () => console.log('Disconnected'),
            });
            client.activate();
            stompClientRef.current = client;
        };

        const timeoutId = setTimeout(setupWebSocket, 500);

        const handleBeforeUnload = () => {
            if (stompClientRef.current?.connected) {
                stompClientRef.current.publish({
                    destination: `/app/room/${roomId}/leave`,
                    body: JSON.stringify({userId: user.id})
                });
            }
        };
        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            clearTimeout(timeoutId);
            handleBeforeUnload();
            if (client) client.deactivate();
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [roomId, user, error, loading]);

    const loadEpisodesForSeason = async (seasonId) => {
        if (episodesBySeason[seasonId] && episodesBySeason[seasonId].length > 0) {
            return episodesBySeason[seasonId];
        }
        try {
            const res = await axios.get(`${STORAGE_API_URL}/series/seasons/${seasonId}/episodes?size=100`);
            const episodes = res.data.content || res.data || [];
            episodes.sort((a, b) => a.episodeNumber - b.episodeNumber);
            setEpisodesBySeason(prev => ({...prev, [seasonId]: episodes}));
            return episodes;
        } catch (err) {
            console.error("Failed to load episodes for season", err);
            return [];
        }
    };

    const handleSyncMessage = (data) => {
        if (data.userId === user.id) return;
        const isPlaying = data.action === 'play';
        playerRef.current?.syncVideoState(data.action, data.currentTime, isPlaying, data.userId);
    };

    const handleInitialStateSync = async (state) => {
        if (state.currentEpisodeId && room?.roomType === 'SERIES') {
        }
        playerRef.current?.syncVideoState('seek', state.currentTime, state.isPlaying, state.lastActionUserId);
    };

    const handleEpisodeChange = async (data) => {
        setRoom(prev => ({
            ...prev,
            currentEpisodeId: data.episodeId,
            currentSeasonId: data.seasonId,
            currentTime: 0,
            isPlaying: false
        }));

        if (data.seasonId !== currentSeasonId) {
            setCurrentSeasonId(data.seasonId);
            await loadEpisodesForSeason(data.seasonId);
        }

        const episodes = await loadEpisodesForSeason(data.seasonId);
        const foundEpisode = episodes.find(e => e.id === data.episodeId);

        if (foundEpisode) {
            setCurrentEpisode(foundEpisode);
            setStreamUrl(`${STORAGE_API_URL}/stream/episodes/${foundEpisode.id}`);
        }

        playerRef.current?.syncVideoState('seek', 0, false, data.userId);
    };

    const sendMessage = (action, currentTime = null, payload = {}) => {
        if (!stompClientRef.current?.connected) return;
        stompClientRef.current.publish({
            destination: `/app/room/${roomId}/${action}`,
            body: JSON.stringify({userId: user.id, currentTime, ...payload})
        });
    };

    useEffect(() => {
        const interval = setInterval(() => {
            if (stompClientRef.current?.connected && playerRef.current) {
                const {currentTime, isPaused} = playerRef.current.getState();
                if (!isPaused) {
                    stompClientRef.current.publish({
                        destination: `/app/room/${roomId}/timeUpdate`,
                        body: JSON.stringify({userId: user.id, currentTime})
                    });
                }
            }
        }, 2000);
        return () => clearInterval(interval);
    }, [roomId, user.id]);

    const switchToEpisode = (episode) => {
        if (!isHost) return;
        sendMessage('switchEpisode', 0, {
            episodeId: episode.id,
            seasonId: episode.seasonId
        });
    };

    const switchToSeason = (seasonId) => {
        if (!isHost) return;
        setCurrentSeasonId(seasonId);
        loadEpisodesForSeason(seasonId);
    };

    const findNextEpisode = () => {
        if (!currentEpisode) return null;
        const currentList = episodesBySeason[currentSeasonId] || [];
        const currentIndex = currentList.findIndex(e => e.id === currentEpisode.id);
        if (currentIndex >= 0 && currentIndex < currentList.length - 1) {
            return currentList[currentIndex + 1];
        }
        return null;
    };

    const findPrevEpisode = () => {
        if (!currentEpisode) return null;
        const currentList = episodesBySeason[currentSeasonId] || [];
        const currentIndex = currentList.findIndex(e => e.id === currentEpisode.id);
        if (currentIndex > 0) return currentList[currentIndex - 1];
        return null;
    };

    const handleNextEpisode = async () => {
        const nextInSeason = findNextEpisode();
        if (nextInSeason) {
            switchToEpisode(nextInSeason);
            return;
        }

        const currentSeasonIndex = availableSeasons.findIndex(s => s.id === currentSeasonId);
        if (currentSeasonIndex !== -1 && currentSeasonIndex < availableSeasons.length - 1) {
            const nextSeason = availableSeasons[currentSeasonIndex + 1];
            try {
                const nextSeasonEpisodes = await loadEpisodesForSeason(nextSeason.id);
                if (nextSeasonEpisodes && nextSeasonEpisodes.length > 0) {
                    const firstEp = nextSeasonEpisodes[0];
                    switchToEpisode(firstEp);
                } else {
                    alert(`Season ${nextSeason.seasonNumber} is empty.`);
                }
            } catch (e) {
                console.error("Failed to load next season episodes", e);
                alert("Error loading next season.");
            }
        } else {
            alert("No more episodes found. This is the end of the series.");
        }
    };

    const handlePrevEpisode = async () => {
        const prevInSeason = findPrevEpisode();
        if (prevInSeason) {
            switchToEpisode(prevInSeason);
            return;
        }

        const currentSeasonIndex = availableSeasons.findIndex(s => s.id === currentSeasonId);
        if (currentSeasonIndex > 0) {
            const prevSeason = availableSeasons[currentSeasonIndex - 1];
            try {
                const prevSeasonEpisodes = await loadEpisodesForSeason(prevSeason.id);
                if (prevSeasonEpisodes && prevSeasonEpisodes.length > 0) {
                    const lastEp = prevSeasonEpisodes[prevSeasonEpisodes.length - 1];
                    switchToEpisode(lastEp);
                } else {
                    alert(`Season ${prevSeason.seasonNumber} appears to be empty.`);
                }
            } catch (e) {
                console.error("Failed to load prev season episodes", e);
                alert("Error loading previous season.");
            }
        } else {
            alert("This is the first episode of the series.");
        }
    };

    if (loading) {
        return <div className="text-center mt-5 text-white"><Spinner animation="border" variant="primary"/><p
            className="mt-2">Loading...</p></div>;
    }
    if (error) {
        return <Alert variant="danger" className="m-5">{error}</Alert>;
    }

    const currentSeasonEpisodes = episodesBySeason[currentSeasonId] || [];
    const allUsers = users.map(u => ({...u, isMe: u.id === user.id}));
    const activeSeason = availableSeasons.find(s => s.id === currentSeasonId);
    const activeSeasonNum = activeSeason ? activeSeason.seasonNumber : '?';
    const displayTitle = room.roomType === 'SERIES' && currentEpisode
        ? `${room.contentTitle} - S${activeSeasonNum}E${currentEpisode.episodeNumber}: ${currentEpisode.title}`
        : room.contentTitle;

    return (
        <Container
            fluid
            className="vh-100 p-0"
            style={{
                backgroundImage: backdropUrl
                    ? `linear-gradient(rgba(15, 23, 42, 0.50), rgba(15, 23, 42, 0.70)), url(${backdropUrl})`
                    : 'none',
                backgroundSize: 'cover',
                backgroundPosition: 'center top',
                backgroundAttachment: 'fixed',
                transition: 'background-image 0.5s ease-in-out'
            }}
        >
            <Row className="room-container flex-grow-1 h-100 mx-0" style={{background: 'transparent'}}>
                <Col md={9} className="main-content p-0 d-flex flex-column" style={{overflowY: 'auto'}}>
                    <div className="container-fluid px-0 pt-3" style={{maxWidth: '1200px', margin: '0 auto'}}>
                        <SyncPlayer
                            ref={playerRef}
                            room={{...room, streamUrl}}
                            user={user}
                            sendMessage={sendMessage}
                            isHost={isHost}
                            currentEpisode={currentEpisode}
                            setCurrentEpisode={setCurrentEpisode}
                        />
                        <Card
                            className="content-info glass-panel my-4 text-white shadow-lg border-0">
                            <Card.Body className="p-4">
                                <Row className="align-items-center">
                                    {/* Cover Image */}
                                    <Col md={2} className="d-none d-md-block">
                                        <div className="position-relative"
                                             style={{aspectRatio: '2/3', overflow: 'hidden', borderRadius: '8px'}}>
                                            <img
                                                src={coverUrl || '/images/default-cover.png'}
                                                className="w-100 h-100 object-fit-cover"
                                                alt="Cover"
                                                onError={(e) => e.target.src = '/images/default-cover.png'}
                                            />
                                        </div>
                                    </Col>
                                    <Col md={10}>
                                        <h3 className="mb-2 text-white fw-bold">{displayTitle}</h3>
                                        {room.roomType === 'SERIES' && currentEpisode && (
                                            <div className="mb-3">
                                                <Badge bg="primary" className="me-2 fs-6">
                                                    Season {activeSeasonNum}
                                                </Badge>
                                                <Badge bg="secondary" className="fs-6">
                                                    Episode {currentEpisode.episodeNumber}
                                                </Badge>
                                            </div>
                                        )}

                                        <hr className="border-secondary border-opacity-25 my-3"/>

                                        <Row className="align-items-end">
                                            <Col md={6}>
                                                <p className="mb-1 text-secondary small text-uppercase fw-bold">Source
                                                    Link</p>
                                                <a href={streamUrl} target="_blank" rel="noreferrer"
                                                   className="text-decoration-none d-flex align-items-center">
                                                    <i className="fas fa-external-link-alt me-2"></i>
                                                    <span className="text-primary text-truncate"
                                                          style={{maxWidth: '300px'}}>
                                                        {streamUrl || 'No link available'}
                                                    </span>
                                                </a>
                                            </Col>
                                            <Col md={6}>
                                                <div className="d-flex justify-content-md-end gap-4 mt-3 mt-md-0">
                                                    <div className="text-md-end">
                                                        <p className="mb-0 text-secondary small text-uppercase fw-bold">Viewers</p>
                                                        <div className="fs-5 fw-bold text-white">
                                                            <i className="fas fa-users text-success me-2"></i>
                                                            {users.length}
                                                        </div>
                                                    </div>
                                                    <div className="text-md-end">
                                                        <p className="mb-0 text-secondary small text-uppercase fw-bold">Room
                                                            ID</p>
                                                        <div className="fs-5 fw-bold text-white font-monospace">
                                                            <span className="user-select-all">{room.id}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </Col>
                                        </Row>
                                    </Col>
                                </Row>
                            </Card.Body>
                        </Card>
                    </div>
                </Col>

                <Col md={3} className="sidebar-col p-0">
                    <div className="d-flex flex-column h-100">
                        {isHost && room.roomType === 'SERIES' && (
                            <div className="p-3 border-bottom border-secondary border-opacity-25 d-none d-md-block">
                                <h6 className="text-uppercase text-secondary small fw-bold mb-3">
                                    <i className="fas fa-sliders-h me-2"></i>Controls
                                </h6>
                                <div className="btn-group w-100">
                                    <Button variant="outline-secondary" size="sm" onClick={handlePrevEpisode}>
                                        <i className="fas fa-step-backward"></i> Prev
                                    </Button>
                                    <Button variant="outline-secondary" size="sm" onClick={handleNextEpisode}>
                                        Next <i className="fas fa-step-forward"></i>
                                    </Button>
                                </div>
                            </div>
                        )}

                        {room.roomType === 'SERIES' && (
                            <div className="p-3 d-flex flex-column">
                                <h6 className="text-uppercase text-secondary small fw-bold mb-3">Episodes</h6>

                                {availableSeasons.length === 0 ? (
                                    <div className="text-center text-secondary my-4">
                                        <small>No seasons loaded</small>
                                    </div>
                                ) : (
                                    <>
                                        <div className="season-tabs d-flex overflow-auto pb-2 mb-2 gap-2">
                                            {availableSeasons.map(season => (
                                                <Button
                                                    key={season.id}
                                                    variant={currentSeasonId === season.id ? 'primary' : 'outline-secondary'}
                                                    size="sm"
                                                    className="season-tab text-nowrap"
                                                    onClick={() => switchToSeason(season.id)}
                                                >
                                                    Season {season.seasonNumber}
                                                </Button>
                                            ))}
                                        </div>

                                        <div className="sidebar-scrollable" style={{maxHeight: '400px'}}>
                                            <ListGroup variant="flush" className="episode-list">
                                                {currentSeasonEpisodes.map(episode => (
                                                    <ListGroup.Item
                                                        key={episode.id}
                                                        action
                                                        onClick={() => isHost ? switchToEpisode(episode) : null}
                                                        className={`episode-item border-0 ${currentEpisode?.id === episode.id ? 'active' : ''}`}
                                                        style={{cursor: isHost ? 'pointer' : 'default'}}
                                                    >
                                                        <div
                                                            className="d-flex justify-content-between align-items-center">
                                                            <div className="overflow-hidden">
                                                                <div
                                                                    className="episode-number">Episode {episode.episodeNumber}</div>
                                                                <div className="episode-title text-truncate">
                                                                    {episode.title || 'Untitled'}
                                                                </div>
                                                            </div>
                                                            <small className="text-muted ms-2"
                                                                   style={{fontSize: '11px', whiteSpace: 'nowrap'}}>
                                                                {formatTime(episode.duration)}
                                                            </small>
                                                        </div>
                                                    </ListGroup.Item>
                                                ))}
                                                {currentSeasonEpisodes.length === 0 && (
                                                    <div className="text-center text-muted small mt-3">No episodes
                                                        found</div>
                                                )}
                                            </ListGroup>
                                        </div>
                                    </>
                                )}
                            </div>
                        )}

                        <div
                            className={`p-3 border-top border-secondary border-opacity-25 ${room.roomType !== 'SERIES' ? 'flex-grow-1 d-flex flex-column min-vh-0' : ''}`}
                            style={room.roomType === 'SERIES' ? {maxHeight: '40%'} : {}}
                        >
                            <h6 className="text-uppercase text-secondary small fw-bold mb-3">
                                <i className="fas fa-users me-2"></i>Viewers
                            </h6>
                            <div
                                className="sidebar-scrollable"
                                style={room.roomType === 'SERIES' ? {maxHeight: '200px'} : {flex: 1, overflowY: 'auto'}}
                            >
                                {allUsers.map(u => (
                                    <Card key={u.id} className="user-item mb-2 text-white border-0"
                                          style={{background: 'rgba(0,0,0,0.3)'}}>
                                        <Card.Body className="p-2 d-flex align-items-center w-100">
                                            <div className="user-avatar me-2">{u.username.charAt(0).toUpperCase()}</div>
                                            <div className="flex-grow-1 overflow-hidden">
                                                <div className="fw-bold text-white text-truncate"
                                                     style={{fontSize: '14px'}}>
                                                    {u.username} {u.isMe &&
                                                    <span className="text-muted small">(You)</span>}
                                                    {room.hostId === u.id && <Badge bg="warning" text="dark"
                                                                                    className="ms-1 rounded-pill">Host</Badge>}
                                                </div>
                                                <small className="text-secondary" style={{fontSize: '11px'}}>
                                                    <i className="fas fa-clock me-1"></i>{formatTime(u.currentTime || 0)}
                                                </small>
                                            </div>
                                        </Card.Body>
                                    </Card>
                                ))}
                            </div>
                        </div>

                        <div className="p-3 border-top border-secondary border-opacity-25">
                            <Button variant="outline-danger" size="sm" className="w-100" onClick={() => {
                                axios.post(`${SYNC_API_URL}/rooms/${roomId}/leave?userId=${user.id}`).finally(() => navigate('/'));
                            }}>
                                <i className="fas fa-sign-out-alt me-2"></i>Leave Room
                            </Button>
                        </div>
                    </div>
                </Col>
            </Row>
        </Container>
    );
};

export default Room;
