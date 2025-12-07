import React, {useEffect, useState} from 'react';
import {Alert, Button, Form, Modal} from 'react-bootstrap';
import axios from 'axios';
import {STORAGE_API_URL, SYNC_API_URL} from '../api';

const RoomModals = ({user, show, onHide, content, contentType, onRoomCreated}) => {
    const [roomName, setRoomName] = useState('');
    const [videoUrl, setVideoUrl] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (content && content.title) {
            setRoomName(`${content.title} - Room`);
        } else {
            setRoomName('');
        }
        setVideoUrl('');
        setError(null);
    }, [content, show]);

    const getButtonVariant = () => contentType === 'SERIES' ? 'success' : 'primary';

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            const payload = {
                roomName: roomName,
                userId: user.id
            };
            let endpoint = '';

            if (contentType === 'MOVIE') {
                endpoint = '/rooms/movie';
                payload.movieId = content.id;
                payload.title = content.title;

            } else if (contentType === 'SERIES') {
                endpoint = '/rooms/series';
                payload.seriesId = content.id;
                payload.title = content.title;

                try {
                    const seasonsRes = await axios.get(`${STORAGE_API_URL}/series/${content.id}/seasons?size=100`);
                    const seasons = seasonsRes.data.content || seasonsRes.data || [];

                    if (seasons.length === 0) throw new Error("No seasons found");
                    seasons.sort((a, b) => a.seasonNumber - b.seasonNumber);
                    const firstSeason = seasons[0];

                    const episodesRes = await axios.get(`${STORAGE_API_URL}/series/seasons/${firstSeason.id}/episodes?size=100`);
                    const episodes = episodesRes.data.content || episodesRes.data || [];

                    if (episodes.length === 0) throw new Error("No episodes in season 1");
                    episodes.sort((a, b) => a.episodeNumber - b.episodeNumber);
                    const firstEpisode = episodes[0];

                    payload.seasonId = firstEpisode.seasonId;
                    payload.episodeId = firstEpisode.id;

                } catch (fetchErr) {
                    console.error("Error finding first episode:", fetchErr);
                    throw new Error("Could not find a playable episode for this series.");
                }

            } else if (contentType === 'CUSTOM') {
                endpoint = '/rooms/custom';
                payload.videoUrl = videoUrl;
            }

            const response = await axios.post(`${SYNC_API_URL}${endpoint}`, payload, {
                headers: {'Content-Type': 'application/json'}
            });
            onRoomCreated(response.data.id);

        } catch (err) {
            console.error('Room creation failed:', err);
            setError(err.response?.data?.error || err.message || 'Failed to create room.');
            setLoading(false);
        }
    };

    const getTitle = () => {
        switch (contentType) {
            case 'MOVIE':
                return 'Create Movie Room';
            case 'SERIES':
                return 'Create Series Room';
            case 'CUSTOM':
                return 'Create Link Room';
            default:
                return 'Create Room';
        }
    };

    return (
        <Modal show={show} onHide={onHide} centered contentClassName="bg-dark text-white">
            <Modal.Header closeButton closeVariant="white">
                <Modal.Title>{getTitle()}</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {error && <Alert variant="danger">{error}</Alert>}

                    {contentType !== 'CUSTOM' && content && (
                        <div className="mb-3">
                            <Form.Label className="text-secondary small text-uppercase fw-bold">Selected
                                Content</Form.Label>
                            <p className={`fs-5 fw-bold ${contentType === 'SERIES' ? 'text-success' : 'text-primary'}`}>{content.title}</p>
                        </div>
                    )}

                    <Form.Group className="mb-3">
                        <Form.Label className="text-secondary small text-uppercase fw-bold">Room Name</Form.Label>
                        <Form.Control
                            type="text"
                            value={roomName}
                            onChange={(e) => setRoomName(e.target.value)}
                            required
                            placeholder="e.g. Friday Horror Night"
                            className="bg-secondary text-white border-secondary"
                        />
                    </Form.Group>

                    {contentType === 'CUSTOM' && (
                        <Form.Group className="mb-3">
                            <Form.Label className="text-secondary small text-uppercase fw-bold">Video URL</Form.Label>
                            <Form.Control
                                type="url"
                                value={videoUrl}
                                onChange={(e) => setVideoUrl(e.target.value)}
                                required
                                placeholder="https://youtube.com/watch?v=... or .mp4 link"
                                className="bg-secondary text-white border-secondary"
                            />
                            <Form.Text className="text-muted small mt-1">
                                Supported: YouTube links or direct MP4/M3U8 links.
                            </Form.Text>
                        </Form.Group>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="outline-light" onClick={onHide}>Cancel</Button>
                    <Button type="submit" variant={getButtonVariant()} disabled={loading}>
                        {loading ? 'Creating...' : 'Create Room'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
};

export default RoomModals;
