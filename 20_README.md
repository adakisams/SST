# ⚡ Sam's Thunder Transfert (STT)

Application Android de transfert P2P de fichiers lourds (vidéos, audio, documents) **sans stockage serveur**, 100% gratuite.

## Fonctionnalités

- Transfert P2P illimité (même Wi-Fi ou Internet mondial)
- Code à 6 chiffres pour connexion
- QR Code
- Historique local
- Favoris
- Mode hors-ligne (Wi-Fi Direct)
- Chiffrement bout-en-bout (WebRTC natif)
- Expiration code 10 min
- Anonyme (pas de compte)
- Reprise de transfert si coupure
- Compression (choix de l'envoyeur)
- Multi-fichiers
- Dossier entier (zip auto)
- Prévisualisation avant acceptation
- Partage depuis autres apps

## Architecture

```
MÊME WI-FI (rapide)
Téléphone A ◄══════════════► Téléphone B
   (envoie)   Wi-Fi Direct    (reçoit)

WI-FI DIFFÉRENTS (Internet)
Téléphone A ◄── Code 847291 ──► Téléphone B
   (envoie)    Firestore = salon   (reçoit)
                temporaire 10 min
                suppression manuelle

        │
        ▼
   Connexion P2P WebRTC directe
   Fichier transféré chiffré
```

## Technologies

- **Kotlin** + **Jetpack Compose**
- **WebRTC** (P2P natif)
- **Firebase Firestore** (plan Spark gratuit — signalisation uniquement)
- **Room** (historique local)
- **Wi-Fi Direct API** (mode hors-ligne)

## Installation

### 1. Prérequis

- Android Studio Hedgehog (2023.1.1) ou plus récent
- SDK Android 34
- JDK 17
- Compte GitHub
- Compte Firebase (plan Spark gratuit)

### 2. Cloner le projet

```bash
git clone https://github.com/votre-compte/STT.git
cd STT
```

### 3. Configurer Firebase

1. Allez sur [Firebase Console](https://console.firebase.google.com)
2. Créez un projet "Sam's Thunder Transfert"
3. Ajoutez une app Android (package : `com.sam.stt`)
4. Téléchargez `google-services.json`
5. Placez-le dans `app/google-services.json`

### 4. Compiler

```bash
./gradlew assembleDebug
```

L'APK sera dans `app/build/outputs/apk/debug/app-debug.apk`

### 5. Tester

Installez l'APK sur **deux téléphones Android** et testez l'envoi/réception.

## Structure des fichiers

```
app/
├── build.gradle.kts              # Dépendances
├── google-services.json          # Config Firebase (à générer)
├── src/main/
│   ├── AndroidManifest.xml       # Permissions
│   ├── java/com/sam/stt/
│   │   ├── MainActivity.kt       # Point d'entrée
│   │   ├── STTApplication.kt     # Initialisation
│   │   ├── model/
│   │   │   └── TransferSession.kt
│   │   ├── data/
│   │   │   ├── STTDatabase.kt
│   │   │   └── TransferDao.kt
│   │   ├── network/
│   │   │   └── FirebaseSignaling.kt
│   │   ├── webrtc/
│   │   │   └── WebRTCManager.kt
│   │   ├── transfer/
│   │   │   └── FileTransferManager.kt
│   │   ├── wifi/
│   │   │   └── WifiDirectManager.kt
│   │   ├── ui/
│   │   │   ├── theme/Color.kt
│   │   │   ├── screens/
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   ├── SendScreen.kt
│   │   │   │   └── ReceiveScreen.kt
│   │   │   └── components/
│   │   │       └── TransferProgress.kt
│   └── res/
│       ├── values/
│       │   ├── themes.xml
│       │   ├── colors.xml
│       │   └── strings.xml
│       └── values-en/
│           └── strings.xml
```

## Coût

| Service | Coût |
|---------|------|
| Transfert P2P | **0 FCFA** |
| Signalisation Firestore | **0 FCFA** (plan Spark) |
| Historique local | **0 FCFA** (stockage téléphone) |
| Wi-Fi Direct | **0 FCFA** (pas de serveur) |

## Langues supportées

- Français
- English
- 日本語 (Japonais)
- Deutsch (Allemand)
- Kabiyé
- Éwé
- Nawdm

## Licence

MIT — 100% gratuit, open source.

---

**Développé avec ⚡ par Sam**
