import { initializeApp, getApps, getApp } from "firebase/app"
import { getAuth, GoogleAuthProvider } from "firebase/auth"
import { getDatabase } from "firebase/database"
import { getStorage } from "firebase/storage"

const firebaseConfig = {
  apiKey: "AIzaSyBQ9oIGJwamcd3__6NNqd_Ds-SAyYoicpY",
  authDomain: "himatsms.firebaseapp.com",
  databaseURL: "https://himatsms-default-rtdb.firebaseio.com",
  projectId: "himatsms",
  storageBucket: "himatsms.firebasestorage.app",
  messagingSenderId: "787473572186",
  appId: "1:787473572186:web:1c6baa198c66cca6f79363",
}

// Initialize Firebase
const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig)
export const auth = getAuth(app)
export const googleProvider = new GoogleAuthProvider()
googleProvider.setCustomParameters({ prompt: 'select_account' })

export const rtdb = getDatabase(app, "https://himatsms-default-rtdb.firebaseio.com")
export const storage = getStorage(app, "gs://himatsms.firebasestorage.app")
export default app

