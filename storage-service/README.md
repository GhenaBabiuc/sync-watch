# MinIO Webhook Configuration Guide

## 📋 Overview
This guide will help you configure webhook notifications in MinIO through the web interface for automatic processing of uploaded files in your Spring Boot application.

## 🚀 Prerequisites

1. Running MinIO server via Docker:
   ```bash
   docker-compose up -d
   ```

2. Running Spring Boot application on port 8080

## 🔧 Step-by-Step Configuration

### Step 1: Access MinIO Console

1. Open your browser and navigate to http://localhost:9001
2. Login with credentials:
    - **Username**: `minio_access_key`
    - **Password**: `minio_secret_key`

### Step 2: Create Bucket

1. In the left menu, select **"Buckets"**
2. Click **"Create Bucket"** button
3. Enter bucket name: `movie-storage`
4. Click **"Create Bucket"**

### Step 3: Configure Webhook Destination

1. In the main menu go to **"Administrator"** → **"Settings"**
2. Find the **"Notifications"** section
3. Click **"Add Event Destination"**
4. Select **"Webhook"** from the destination types

#### Webhook Parameters:
- **Identifier**: `my-webhook`
- **Endpoint**: `http://host.docker.internal:8080/api/webhooks/minio/notification`
- **Auth Token**: leave empty
- **Queue Directory**: `/tmp/minio-events`
- **Queue Limit**: `1000`

5. Click **"Save Event Destination"**

### Step 4: Configure Event Notification for Bucket

1. Go back to **"Buckets"** section
2. Find and open the `movie-storage` bucket
3. Navigate to the **"Events"** tab
4. Click **"Subscribe To Bucket Events"**

#### Event Subscription Parameters:
- **ARN**: `arn:minio:sqs::my-webhook:webhook`
- **Prefix**: leave empty (for all files)
- **Suffix**: leave empty (for all extensions)
- **Select Event**: check ✅ **PUT - Object Uploaded**

5. Click **"Save"**
