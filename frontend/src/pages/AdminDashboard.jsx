import React, {useEffect, useState} from 'react';
import axios from 'axios';
import {Accordion, Badge, Button, Card, Container, Form, Modal, ProgressBar, Tab, Tabs} from 'react-bootstrap';
import PaginationControl from '../components/PaginationControl';
import {STORAGE_API_URL} from '../api';

const AdminDashboard = () => {
    const [moviePage, setMoviePage] = useState({content: [], totalPages: 0, number: 0});
    const [seriesPage, setSeriesPage] = useState({content: [], totalPages: 0, number: 0});
    const [uploadQueue, setUploadQueue] = useState([]);
    const [refreshTriggers, setRefreshTriggers] = useState({});
    const [showModal, setShowModal] = useState(false);
    const [modalType, setModalType] = useState('MOVIE');
    const [modalData, setModalData] = useState({});
    const [parentId, setParentId] = useState(null);

    const triggerRefresh = (key) => {
        setRefreshTriggers(prev => ({...prev, [key]: Date.now()}));
    };

    const fetchMovies = async (page = 0) => {
        try {
            const res = await axios.get(`${STORAGE_API_URL}/movies`, {params: {page, size: 20}});
            setMoviePage(res.data);
        } catch (e) {
            console.error(e);
        }
    };

    const fetchSeries = async (page = 0) => {
        try {
            const res = await axios.get(`${STORAGE_API_URL}/series`, {params: {page, size: 20}});
            setSeriesPage(res.data);
        } catch (e) {
            console.error(e);
        }
    };

    const handleMoviePageChange = (page) => {
        fetchMovies(page);
        window.scrollTo({top: 0, behavior: 'smooth'});
    };

    const handleSeriesPageChange = (page) => {
        fetchSeries(page);
        window.scrollTo({top: 0, behavior: 'smooth'});
    };

    useEffect(() => {
        fetchMovies(0);
        fetchSeries(0);
    }, []);

    const handleFileUpload = async (files, entityType, entityId, category, parentId = null) => {
        if (!files || files.length === 0) return;

        const newTasks = Array.from(files).map(file => ({
            id: Math.random().toString(36).substr(2, 9),
            file,
            name: file.name,
            progress: 0,
            status: 'PENDING',
            entityType,
            entityId,
            category,
            parentId
        }));

        setUploadQueue(prev => [...prev, ...newTasks]);
        newTasks.forEach(task => processUpload(task));
    };

    const processUpload = async (task) => {
        updateTaskStatus(task.id, 'UPLOADING', 0);
        try {
            const initRes = await axios.post(`${STORAGE_API_URL}/files/upload`, {
                originalFilename: task.file.name,
                mimeType: task.file.type || 'application/octet-stream',
                fileSize: task.file.size,
                entityId: task.entityId,
                entityType: task.entityType,
                category: task.category,
                isPrimary: task.category === 'POSTER'
            });

            const {presignedUrl, minioObjectKey} = initRes.data;

            await axios.put(presignedUrl, task.file, {
                headers: {'Content-Type': task.file.type},
                onUploadProgress: (progressEvent) => {
                    const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    updateTaskStatus(task.id, 'UPLOADING', percent);
                }
            });

            await axios.post(`${STORAGE_API_URL}/webhooks/minio/notification`, {
                "Records": [{
                    "eventName": "s3:ObjectCreated:Put",
                    "s3": {"bucket": {"name": "movie-storage"}, "object": {"key": minioObjectKey}}
                }]
            });

            updateTaskStatus(task.id, 'COMPLETED', 100);

            if (task.entityType === 'MOVIE') {
                fetchMovies(moviePage.number);
            } else if (task.entityType === 'SERIES') {
                fetchSeries(seriesPage.number);
            } else if (task.entityType === 'EPISODE' && task.parentId) {
                triggerRefresh(`season-${task.parentId}`);
            }

        } catch (error) {
            console.error("Upload failed", error);
            updateTaskStatus(task.id, 'ERROR', 0);
        }
    };

    const updateTaskStatus = (id, status, progress) => {
        setUploadQueue(prev => prev.map(t => t.id === id ? {...t, status, progress} : t));
    };

    const openModal = (type, data = null, pId = null) => {
        setModalType(type);
        setModalData(data || {});
        setParentId(pId);
        setShowModal(true);
    };

    const handleSave = async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        if (data.year) data.year = parseInt(data.year);
        if (data.duration) data.duration = parseInt(data.duration);
        if (data.seasonNumber) data.seasonNumber = parseInt(data.seasonNumber);
        if (data.episodeNumber) data.episodeNumber = parseInt(data.episodeNumber);

        let url = '';
        let method = modalData.id ? 'put' : 'post';

        if (modalType === 'MOVIE') url = modalData.id ? `/movies/${modalData.id}` : '/movies';
        else if (modalType === 'SERIES') url = modalData.id ? `/series/${modalData.id}` : '/series';
        else if (modalType === 'SEASON') {
            data.seriesId = parentId;
            url = modalData.id ? `/series/seasons/${modalData.id}` : `/series/${parentId}/seasons`;
        } else if (modalType === 'EPISODE') {
            data.seasonId = parentId;
            url = modalData.id ? `/series/episodes/${modalData.id}` : `/series/seasons/${parentId}/episodes`;
        }

        try {
            await axios({method, url: `${STORAGE_API_URL}${url}`, data});
            setShowModal(false);

            if (modalType === 'MOVIE') fetchMovies(moviePage.number);
            else if (modalType === 'SERIES') fetchSeries(seriesPage.number);
            else if (modalType === 'SEASON') triggerRefresh(`series-${parentId}`);
            else if (modalType === 'EPISODE') triggerRefresh(`season-${parentId}`);

        } catch (err) {
            alert('Error saving: ' + err.message);
        }
    };

    const handleDelete = async (type, id, pId = null) => {
        if (!confirm('Are you sure? This will delete all files too.')) return;
        try {
            let endpoint = type === 'MOVIE' ? 'movies' : type === 'SERIES' ? 'series' : type === 'SEASON' ? 'series/seasons' : 'series/episodes';
            await axios.delete(`${STORAGE_API_URL}/${endpoint}/${id}`);

            if (type === 'MOVIE') fetchMovies(moviePage.number);
            else if (type === 'SERIES') fetchSeries(seriesPage.number);
            else if (type === 'SEASON') triggerRefresh(`series-${pId}`);
            else if (type === 'EPISODE') triggerRefresh(`season-${pId}`);
        } catch (err) {
            alert('Delete failed');
        }
    };

    const StatusBadge = ({files, type}) => {
        if (!files) return null;
        const has = (cat) => files.some(f => f.category === cat && f.uploadStatus === 'COMPLETED');
        return (
            <div className="d-flex gap-1 flex-wrap">
                {type === 'VIDEO' && (has('VIDEO') ? <Badge bg="success">Video</Badge> :
                    <Badge bg="danger">No Video</Badge>)}
                {has('POSTER') ? <Badge bg="success">Poster</Badge> : <Badge bg="secondary">No Poster</Badge>}
                {has('BACKDROP') ? <Badge bg="success">Backdrop</Badge> : <Badge bg="secondary">No Back</Badge>}
            </div>
        );
    };

    const UploadZone = ({type, id, parentId}) => {
        const [category, setCategory] = useState(type === 'EPISODE' || type === 'MOVIE' ? 'VIDEO' : 'POSTER');
        return (
            <div className="border border-secondary rounded p-2 text-center"
                 onDragOver={e => e.preventDefault()}
                 onDrop={e => {
                     e.preventDefault();
                     handleFileUpload(e.dataTransfer.files, type, id, category, parentId);
                 }}>
                <Form.Select size="sm" className="mb-1 bg-dark text-white border-secondary"
                             value={category} onChange={e => setCategory(e.target.value)}>
                    {(type === 'MOVIE' || type === 'EPISODE') && <option value="VIDEO">Video File</option>}
                    <option value="POSTER">Poster/Thumbnail</option>
                    <option value="BACKDROP">Backdrop Image</option>
                </Form.Select>
                <div className="small text-muted">Drag & Drop file here</div>
            </div>
        );
    };

    return (
        <Container fluid className="py-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h2>Admin Dashboard</h2>
            </div>

            {uploadQueue.length > 0 && (
                <Card className="mb-4 bg-dark text-white border-secondary">
                    <Card.Header>Active Uploads</Card.Header>
                    <Card.Body style={{maxHeight: '200px', overflowY: 'auto'}}>
                        {uploadQueue.map(task => (
                            <div key={task.id} className="mb-2">
                                <div className="d-flex justify-content-between small">
                                    <span>{task.name}</span>
                                    <span>{task.status}</span>
                                </div>
                                <ProgressBar now={task.progress}
                                             variant={task.status === 'ERROR' ? 'danger' : 'success'} height="5px"/>
                            </div>
                        ))}
                    </Card.Body>
                </Card>
            )}

            <Tabs defaultActiveKey="movies" className="mb-3">
                <Tab eventKey="movies" title="Movies">
                    <Button variant="success" className="mb-3" onClick={() => openModal('MOVIE')}>+ Add Movie</Button>
                    <div className="d-flex flex-column gap-3">
                        {moviePage.content.map(movie => (
                            <Card key={movie.id} className="bg-dark text-white border-secondary">
                                <Card.Body className="d-flex align-items-center gap-3">
                                    <div style={{width: '50px'}}>#{movie.id}</div>
                                    <div className="flex-grow-1">
                                        <div className="fw-bold">{movie.title}</div>
                                        <div className="small text-muted">{movie.year} • {movie.duration} min</div>
                                    </div>
                                    <StatusBadge files={movie.mediaFiles} type="VIDEO"/>
                                    <div style={{minWidth: '150px'}}>
                                        <UploadZone type="MOVIE" id={movie.id}/>
                                    </div>
                                    <div className="d-flex flex-column gap-1">
                                        <Button size="sm" variant="outline-primary"
                                                onClick={() => openModal('MOVIE', movie)}>Edit</Button>
                                        <Button size="sm" variant="outline-danger"
                                                onClick={() => handleDelete('MOVIE', movie.id)}>Del</Button>
                                    </div>
                                </Card.Body>
                            </Card>
                        ))}
                    </div>
                    <PaginationControl
                        currentPage={moviePage.number}
                        totalPages={moviePage.totalPages}
                        onPageChange={handleMoviePageChange}
                    />
                </Tab>

                <Tab eventKey="series" title="Series">
                    <Button variant="success" className="mb-3" onClick={() => openModal('SERIES')}>+ Add Series</Button>
                    <Accordion>
                        {seriesPage.content.map(s => (
                            <Accordion.Item eventKey={s.id.toString()} key={s.id}
                                            className="bg-dark text-white border-secondary mb-2">
                                <Accordion.Header>
                                    <div className="d-flex justify-content-between w-100 me-3 align-items-center">
                                        <span>{s.title} ({s.year})</span>
                                        <StatusBadge files={s.mediaFiles}/>
                                    </div>
                                </Accordion.Header>
                                <Accordion.Body className="bg-dark text-white">
                                    <div
                                        className="d-flex justify-content-end gap-2 mb-3 border-bottom border-secondary pb-2">
                                        <div style={{minWidth: '200px'}}>
                                            <UploadZone type="SERIES" id={s.id}/>
                                        </div>
                                        <Button size="sm" variant="outline-primary"
                                                onClick={() => openModal('SERIES', s)}>Edit Series</Button>
                                        <Button size="sm" variant="outline-danger"
                                                onClick={() => handleDelete('SERIES', s.id)}>Del Series</Button>
                                        <Button size="sm" variant="success"
                                                onClick={() => openModal('SEASON', null, s.id)}>+ Add Season</Button>
                                    </div>

                                    <SeasonsList
                                        seriesId={s.id}
                                        openModal={openModal}
                                        handleDelete={handleDelete}
                                        UploadZone={UploadZone}
                                        StatusBadge={StatusBadge}
                                        refreshTrigger={refreshTriggers[`series-${s.id}`]}
                                        refreshTriggers={refreshTriggers}
                                    />
                                </Accordion.Body>
                            </Accordion.Item>
                        ))}
                    </Accordion>
                    <PaginationControl
                        currentPage={seriesPage.number}
                        totalPages={seriesPage.totalPages}
                        onPageChange={handleSeriesPageChange}
                    />
                </Tab>
            </Tabs>

            <Modal show={showModal} onHide={() => setShowModal(false)} centered
                   contentClassName="bg-dark text-white border-secondary">
                <Modal.Header closeButton closeVariant="white">
                    <Modal.Title>{modalData.id ? 'Edit' : 'Create'} {modalType}</Modal.Title>
                </Modal.Header>
                <Form onSubmit={handleSave}>
                    <Modal.Body>
                        {(modalType === 'SEASON' || modalType === 'EPISODE') && (
                            <Form.Group className="mb-3">
                                <Form.Label>{modalType === 'SEASON' ? 'Season' : 'Episode'} Number</Form.Label>
                                <Form.Control name={modalType === 'SEASON' ? 'seasonNumber' : 'episodeNumber'}
                                              type="number"
                                              defaultValue={modalData.seasonNumber || modalData.episodeNumber}
                                              required/>
                            </Form.Group>
                        )}
                        <Form.Group className="mb-3">
                            <Form.Label>Title</Form.Label>
                            <Form.Control name="title" defaultValue={modalData.title} required/>
                        </Form.Group>
                        <Form.Group className="mb-3">
                            <Form.Label>Description</Form.Label>
                            <Form.Control as="textarea" name="description" defaultValue={modalData.description}/>
                        </Form.Group>
                        {(modalType === 'MOVIE' || modalType === 'SERIES') && (
                            <Form.Group className="mb-3">
                                <Form.Label>Year</Form.Label>
                                <Form.Control name="year" type="number" defaultValue={modalData.year}/>
                            </Form.Group>
                        )}
                        {(modalType === 'MOVIE' || modalType === 'EPISODE') && (
                            <Form.Group className="mb-3">
                                <Form.Label>Duration (min)</Form.Label>
                                <Form.Control name="duration" type="number" defaultValue={modalData.duration}/>
                            </Form.Group>
                        )}
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
                        <Button variant="primary" type="submit">Save</Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </Container>
    );
};

const SeasonsList = ({seriesId, openModal, handleDelete, UploadZone, StatusBadge, refreshTrigger, refreshTriggers}) => {
    const [seasons, setSeasons] = useState([]);

    useEffect(() => {
        axios.get(`${STORAGE_API_URL}/series/${seriesId}/seasons?size=50`)
            .then(res => setSeasons(res.data.content || []));
    }, [seriesId, refreshTrigger]);

    return (
        <div className="ms-3">
            {seasons.map(season => (
                <div key={season.id} className="mb-3 border-start border-success ps-3">
                    <div className="d-flex justify-content-between align-items-center mb-2">
                        <h5 className="mb-0 text-success">Season {season.seasonNumber}: {season.title}</h5>
                        <div className="d-flex gap-2">
                            <Button size="sm" variant="outline-light"
                                    onClick={() => openModal('SEASON', season, seriesId)}>Edit</Button>
                            <Button size="sm" variant="outline-danger"
                                    onClick={() => handleDelete('SEASON', season.id, seriesId)}>Del</Button>
                            <Button size="sm" variant="success" onClick={() => openModal('EPISODE', null, season.id)}>+
                                Add Ep</Button>
                        </div>
                    </div>
                    <EpisodesList
                        seasonId={season.id}
                        openModal={openModal}
                        handleDelete={handleDelete}
                        UploadZone={UploadZone}
                        StatusBadge={StatusBadge}
                        refreshTrigger={refreshTriggers ? refreshTriggers[`season-${season.id}`] : null}
                    />
                </div>
            ))}
        </div>
    );
};

const EpisodesList = ({seasonId, openModal, handleDelete, UploadZone, StatusBadge, refreshTrigger}) => {
    const [episodes, setEpisodes] = useState([]);

    useEffect(() => {
        axios.get(`${STORAGE_API_URL}/series/seasons/${seasonId}/episodes?size=100`)
            .then(res => setEpisodes(res.data.content || []));
    }, [seasonId, refreshTrigger]);

    return (
        <div className="d-flex flex-column gap-2">
            {episodes.map(ep => (
                <Card key={ep.id} className="bg-secondary bg-opacity-25 border-0">
                    <Card.Body className="py-2 d-flex align-items-center gap-3">
                        <div style={{width: '30px'}} className="fw-bold">{ep.episodeNumber}</div>
                        <div className="flex-grow-1 text-truncate">{ep.title}</div>
                        <StatusBadge files={ep.mediaFiles} type="VIDEO"/>
                        <div style={{width: '180px'}}>
                            <UploadZone type="EPISODE" id={ep.id} parentId={seasonId}/>
                        </div>
                        <Button size="sm" variant="link" className="text-white p-0"
                                onClick={() => openModal('EPISODE', ep, seasonId)}><i
                            className="fas fa-edit"></i></Button>
                        <Button size="sm" variant="link" className="text-danger p-0"
                                onClick={() => handleDelete('EPISODE', ep.id, seasonId)}><i
                            className="fas fa-trash"></i></Button>
                    </Card.Body>
                </Card>
            ))}
        </div>
    );
};

export default AdminDashboard;
