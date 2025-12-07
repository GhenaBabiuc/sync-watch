import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Alert, Spinner} from 'react-bootstrap';
import YouTube from 'react-youtube';

const DEBOUNCE_TIME = 300;
const TIME_SKEW_TOLERANCE = 2.0;
const VOLUME_KEY = 'sync_watch_volume';

const SyncPlayer = React.forwardRef(({room, user, sendMessage, isHost, currentEpisode, setCurrentEpisode}, ref) => {
    const videoRef = useRef(null);
    const ytPlayerRef = useRef(null);
    const containerRef = useRef(null);
    const debounceTimeoutRef = useRef(null);
    const pendingSeekTimeRef = useRef(null);
    const [playerType, setPlayerType] = useState('HTML5');
    const [isLoading, setIsLoading] = useState(false);
    const [syncAction, setSyncAction] = useState(false);
    const [debouncedStreamUrl, setDebouncedStreamUrl] = useState(null);

    const getSavedVolume = () => {
        const saved = localStorage.getItem(VOLUME_KEY);
        return saved !== null ? parseFloat(saved) : 1.0;
    };

    useEffect(() => {
        if (!room.streamUrl) {
            setDebouncedStreamUrl(null);
            return;
        }
        const handler = setTimeout(() => {
            setDebouncedStreamUrl(room.streamUrl);
        }, 500);

        return () => clearTimeout(handler);
    }, [room.streamUrl]);

    const activeUrl = debouncedStreamUrl;

    const isYouTubeUrl = (url) => url && (url.includes('youtube.com') || url.includes('youtu.be'));

    const getYouTubeId = (url) => {
        const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
        const match = url.match(regExp);
        return (match && match[2].length === 11) ? match[2] : null;
    };

    const getPlayerState = () => {
        if (playerType === 'YOUTUBE' && ytPlayerRef.current) {
            if (typeof ytPlayerRef.current.getCurrentTime !== 'function') return {currentTime: 0, isPaused: true};
            const state = ytPlayerRef.current.getPlayerState();
            return {
                currentTime: ytPlayerRef.current.getCurrentTime(),
                isPaused: state !== YouTube.PlayerState.PLAYING
            };
        }
        if (playerType === 'HTML5' && videoRef.current) {
            return {
                currentTime: videoRef.current.currentTime,
                isPaused: videoRef.current.paused
            };
        }
        return {currentTime: 0, isPaused: true};
    };

    const playerControls = useMemo(() => ({
        play: () => {
            if (playerType === 'YOUTUBE' && ytPlayerRef.current) ytPlayerRef.current.playVideo();
            else if (playerType === 'HTML5' && videoRef.current) videoRef.current.play().catch(e => console.error('Play error', e));
        },
        pause: () => {
            if (playerType === 'YOUTUBE' && ytPlayerRef.current) ytPlayerRef.current.pauseVideo();
            else if (playerType === 'HTML5' && videoRef.current) videoRef.current.pause();
        },
        setCurrentTime: (time) => {
            if (playerType === 'YOUTUBE' && ytPlayerRef.current) {
                ytPlayerRef.current.seekTo(time, true);
            } else if (playerType === 'HTML5' && videoRef.current) {
                if (videoRef.current.readyState >= 1) {
                    videoRef.current.currentTime = time;
                } else {
                    console.log(`Video not ready, saving pending seek: ${time}`);
                    pendingSeekTimeRef.current = time;
                }
            }
        },
        getState: getPlayerState
    }), [playerType]);

    useEffect(() => {
        const handleCaptureKeyDown = (e) => {
            const tagName = document.activeElement ? document.activeElement.tagName.toUpperCase() : '';
            if (tagName === 'INPUT' || tagName === 'TEXTAREA') return;

            const key = e.key.toLowerCase();
            const controlKeys = [' ', 'k', 'f', 'arrowleft', 'arrowright', 'j', 'l'];

            if (!controlKeys.includes(key)) return;

            e.preventDefault();
            e.stopImmediatePropagation();

            if (key === ' ' || key === 'k') {
                const {isPaused} = playerControls.getState();
                if (isPaused) playerControls.play();
                else playerControls.pause();

            } else if (key === 'f') {
                if (!document.fullscreenElement) {
                    if (playerType === 'HTML5' && videoRef.current) {
                        videoRef.current.requestFullscreen().catch(console.error);
                    }
                    else if (containerRef.current) {
                        containerRef.current.requestFullscreen().catch(console.error);
                    }
                } else {
                    document.exitFullscreen();
                }

            } else if (key === 'arrowleft' || key === 'j') {
                const {currentTime} = playerControls.getState();
                playerControls.setCurrentTime(currentTime - 5);

            } else if (key === 'arrowright' || key === 'l') {
                const {currentTime} = playerControls.getState();
                playerControls.setCurrentTime(currentTime + 5);
            }
        };

        document.addEventListener('keydown', handleCaptureKeyDown, {capture: true});
        return () => {
            document.removeEventListener('keydown', handleCaptureKeyDown, {capture: true});
        };
    }, [playerControls]);

    useEffect(() => {
        if (!activeUrl) {
            setPlayerType('NONE');
            setIsLoading(false);
            return;
        }

        pendingSeekTimeRef.current = null;

        if (isYouTubeUrl(activeUrl)) {
            setPlayerType('YOUTUBE');
            setIsLoading(true);
        } else {
            setPlayerType('HTML5');
            setIsLoading(false);
        }

        if (room.currentTime && room.currentTime > 0) {
            pendingSeekTimeRef.current = room.currentTime;
        }

    }, [activeUrl]);

    useEffect(() => {
        if (playerType !== 'YOUTUBE') return;

        const volumeInterval = setInterval(() => {
            if (ytPlayerRef.current && typeof ytPlayerRef.current.getVolume === 'function') {
                const currentVol = ytPlayerRef.current.getVolume(); // Возвращает 0-100
                const normalizedVol = currentVol / 100;

                if (Math.abs(getSavedVolume() - normalizedVol) > 0.01) {
                    localStorage.setItem(VOLUME_KEY, normalizedVol);
                }
            }
        }, 2000);

        return () => clearInterval(volumeInterval);
    }, [playerType]);

    const sendActionDebounced = (action) => {
        if (debounceTimeoutRef.current) clearTimeout(debounceTimeoutRef.current);
        debounceTimeoutRef.current = setTimeout(() => {
            const {currentTime} = getPlayerState();
            sendMessage(action, currentTime);
        }, DEBOUNCE_TIME);
    };

    const handleHTML5Play = () => {
        if (!syncAction) sendActionDebounced('play');
        setIsLoading(false);
    };
    const handleHTML5Pause = () => {
        if (!syncAction) sendActionDebounced('pause');
    };
    const handleHTML5Seeked = () => {
        if (!syncAction) sendActionDebounced('seek');
    };
    const handleHTML5Waiting = () => setIsLoading(true);
    const handleHTML5CanPlay = () => setIsLoading(false);

    const handleHTML5LoadedMetadata = () => {
        setIsLoading(false);
        const video = videoRef.current;
        if (!video) return;

        video.volume = getSavedVolume();

        if (pendingSeekTimeRef.current !== null) {
            if (Number.isFinite(pendingSeekTimeRef.current)) {
                video.currentTime = pendingSeekTimeRef.current;
            }
            pendingSeekTimeRef.current = null;
        }

        if (room.isPlaying && video.paused) {
            video.play().catch(e => {
                console.warn("Autoplay blocked by browser policy:", e);
            });
        }
    };

    const handleHTML5VolumeChange = (e) => {
        localStorage.setItem(VOLUME_KEY, e.target.volume);
    };

    const handleYouTubeReady = (event) => {
        ytPlayerRef.current = event.target;
        setIsLoading(false);

        event.target.setVolume(getSavedVolume() * 100);

        if (pendingSeekTimeRef.current !== null) {
            event.target.seekTo(pendingSeekTimeRef.current, true);
            pendingSeekTimeRef.current = null;
        }
        if (room.isPlaying) {
            event.target.playVideo();
        }
    };

    const handleYouTubeStateChange = (event) => {
        if (syncAction) return;
        if (event.data === YouTube.PlayerState.PLAYING) sendActionDebounced('play');
        else if (event.data === YouTube.PlayerState.PAUSED) sendActionDebounced('pause');
    };

    const syncVideoState = (action, newTime, newIsPlaying, userId) => {
        setSyncAction(true);
        const {currentTime: localTime} = getPlayerState();
        const timeDiff = Math.abs(localTime - newTime);

        if (playerType === 'HTML5' && videoRef.current && videoRef.current.readyState < 1) {
            pendingSeekTimeRef.current = newTime;
        }

        if (action === 'seek' || timeDiff > TIME_SKEW_TOLERANCE) {
            playerControls.setCurrentTime(newTime);
        }

        if (newIsPlaying && getPlayerState().isPaused) {
            playerControls.play();
        } else if (!newIsPlaying && !getPlayerState().isPaused) {
            playerControls.pause();
        }

        setCurrentEpisode(prev => ({...prev, currentTime: newTime, isPlaying: newIsPlaying, lastActionUserId: userId}));
        setTimeout(() => setSyncAction(false), 500);
    };

    React.useImperativeHandle(ref, () => ({
        syncVideoState,
        getState: getPlayerState,
        controls: playerControls
    }));

    const renderPlayer = () => {
        if (playerType === 'HTML5') {
            return (
                <video
                    ref={videoRef}
                    src={activeUrl}
                    className="video-player"
                    controls
                    preload="metadata"
                    onPlay={handleHTML5Play}
                    onPause={handleHTML5Pause}
                    onSeeked={handleHTML5Seeked}
                    onWaiting={handleHTML5Waiting}
                    onCanPlay={handleHTML5CanPlay}
                    onLoadedMetadata={handleHTML5LoadedMetadata}
                    onVolumeChange={handleHTML5VolumeChange}
                    style={{outline: 'none'}}
                >
                    Your browser does not support video.
                </video>
            );
        } else if (playerType === 'YOUTUBE') {
            const videoId = getYouTubeId(activeUrl);
            if (!videoId) return <Alert variant="warning">Invalid YouTube URL: {activeUrl}</Alert>;
            return (
                <YouTube
                    videoId={videoId}
                    className="video-player"
                    style={{width: '100%', height: '100%'}}
                    opts={{
                        height: '100%',
                        width: '100%',
                        playerVars: {playsinline: 1, controls: 1, rel: 0, origin: window.location.origin}
                    }}
                    onReady={handleYouTubeReady}
                    onStateChange={handleYouTubeStateChange}
                />
            );
        } else {
            return <div className="p-5 text-center text-secondary">No media link configured for this room.</div>;
        }
    };

    return (
        <div
            ref={containerRef}
            className="video-container"
            style={{aspectRatio: '16/9', position: 'relative', backgroundColor: '#000'}}
        >
            {isLoading && (
                <div className="loading-indicator">
                    <Spinner animation="border" variant="primary" role="status"/>
                </div>
            )}
            {renderPlayer()}
        </div>
    );
});

export default SyncPlayer;
