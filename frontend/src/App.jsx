import {BrowserRouter, Route, Routes} from 'react-router-dom';
import {useEffect, useState} from 'react';
import NavBar from './components/NavBar';
import Dashboard from './pages/Dashboard';
import Room from './pages/Room';
import SetUsername from './pages/SetUsername';

function App() {
    const [user, setUser] = useState({id: '', username: ''});
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let storedId = localStorage.getItem('userId');
        let storedName = localStorage.getItem('username');

        if (!storedId) {
            if (typeof crypto !== 'undefined' && crypto.randomUUID) {
                storedId = crypto.randomUUID();
            } else {
                storedId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
                    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
                    return v.toString(16);
                });
            }

            storedName = `Guest_${storedId.substring(0, 4)}`;

            localStorage.setItem('userId', storedId);
            localStorage.setItem('username', storedName);
        }

        setUser({id: storedId, username: storedName});
        setLoading(false);
    }, []);

    if (loading) return null;

    return (
        <BrowserRouter>
            <div className="bg-dark text-white min-vh-100 d-flex flex-column">
                <NavBar user={user}/>

                <Routes>
                    <Route path="/" element={<Dashboard user={user}/>}/>
                    <Route path="/room/:roomId" element={<Room user={user}/>}/>
                    <Route path="/set-username" element={<SetUsername user={user} setUser={setUser}/>}/>
                </Routes>
            </div>
        </BrowserRouter>
    );
}

export default App;
