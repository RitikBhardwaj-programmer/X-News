# X-NEWS

### AI-Powered News Intelligence

X-NEWS is a distributed news intelligence platform that goes beyond
displaying articles. It collects reporting from multiple sources, groups
articles that describe the same real-world event, and uses AI to help
users understand an event rather than simply reading isolated headlines.

The project is built around a **semantic event intelligence pipeline**
using Spring Boot, Kafka, PostgreSQL, pgvector, a Python/FastAPI AI
microservice, sentence embeddings, a learned event-matching model,
Gemini-based analysis, and a React frontend.

> **Current release: X-NEWS V2.0**
>
> V2 introduces semantic event matching, vector retrieval, AI-assisted
> event intelligence, and frontend-integrated
> authentication/authorization.

------------------------------------------------------------------------

## Table of Contents

-   [What Problem Does X-NEWS Solve?](#what-problem-does-x-news-solve)
-   [What Makes X-NEWS Different?](#what-makes-x-news-different)
-   [V2 Highlights](#v2-highlights)
-   [Architecture](#architecture)
-   [End-to-End Data Flow](#end-to-end-data-flow)
-   [Semantic Event Matching](#semantic-event-matching)
-   [AI Microservice](#ai-microservice)
-   [Kafka Processing](#kafka-processing)
-   [Backend](#backend)
-   [Frontend](#frontend)
-   [Authentication and
    Authorization](#authentication-and-authorization)
-   [Database and Vector Search](#database-and-vector-search)
-   [Gemini Event Analysis](#gemini-event-analysis)
-   [Technology Stack](#technology-stack)
-   [Project Structure](#project-structure)
-   [Running X-NEWS Locally](#running-x-news-locally)
-   [Environment Variables](#environment-variables)
-   [API Overview](#api-overview)
-   [Semantic Experiment](#semantic-experiment)
-   [Model Evaluation](#model-evaluation)
-   [V1 vs V2](#v1-vs-v2)
-   [Known Limitations](#known-limitations)
-   [V3 Roadmap](#v3-roadmap)
-   [Engineering Lessons](#engineering-lessons)
-   [License](#license)

------------------------------------------------------------------------

## What Problem Does X-NEWS Solve?

Traditional news applications treat every article as an independent
piece of content.

That creates a problem:

> The same real-world event can appear as dozens of different articles
> from different sources.

For example:

``` text
Source A:
India launches a national AI initiative

Source B:
Government announces major artificial intelligence programme

Source C:
India expands its national AI strategy
```

These are different articles, but they may describe the **same
underlying event**.

X-NEWS attempts to transform:

``` text
Articles
   ↓
Semantic understanding
   ↓
Event matching
   ↓
Event clusters
   ↓
Event intelligence
```

The goal is to make the **event**, rather than the individual article,
the primary unit of news intelligence.

------------------------------------------------------------------------

# What Makes X-NEWS Different?

X-NEWS is designed around several architectural principles:

### 1. Event-centric news

Instead of presenting only:

``` text
Article → Article → Article → Article
```

X-NEWS attempts to create:

``` text
                    EVENT
                      │
          ┌───────────┼───────────┐
          ↓           ↓           ↓
       Source A    Source B    Source C
        article     article     article
```

### 2. Semantic matching

Articles do not need to use exactly the same words to be recognized as
describing the same event.

X-NEWS uses sentence embeddings to represent article meaning.

### 3. Event-aware machine learning

The final event matcher is trained specifically for the question:

> "Do these two articles describe the same event?"

rather than using a generic text classification model.

### 4. Distributed processing

Kafka separates article collection from expensive processing.

### 5. Hybrid architecture

The system combines:

-   deterministic processing
-   pretrained embeddings
-   vector search
-   a learned classifier
-   generative AI

Each technology is used for the problem it is best suited to solve.

------------------------------------------------------------------------

# V2 Highlights

X-NEWS V2 includes:

-   React/Vite frontend
-   Spring Boot backend
-   JWT authentication
-   USER / ADMIN roles
-   PostgreSQL persistence
-   JPA/Hibernate
-   HikariCP connection pooling
-   Confluent Cloud Kafka
-   Asynchronous article processing
-   Article enrichment
-   384-dimensional sentence embeddings
-   pgvector vector storage
-   pgvector nearest-neighbor retrieval
-   Learned semantic event matching
-   Event clustering
-   FastAPI AI microservice
-   Gemini event analysis
-   Frontend authentication integration
-   Local frontend/backend integration
-   Dockerized backend
-   Azure deployment infrastructure carried forward from V1

------------------------------------------------------------------------

# Architecture

``` text
                         ┌──────────────────────────┐
                         │       News Sources       │
                         └────────────┬─────────────┘
                                      │
                                      ▼
                         ┌──────────────────────────┐
                         │    Spring Boot Backend   │
                         │                          │
                         │ NewsCollectionService    │
                         └────────────┬─────────────┘
                                      │
                              Save + Publish
                                      │
                    ┌─────────────────┴─────────────────┐
                    │                                   │
                    ▼                                   ▼
             ┌─────────────┐                    ┌──────────────┐
             │ PostgreSQL  │                    │    Kafka     │
             │             │                    │ xnews.articles│
             └──────▲──────┘                    └──────┬───────┘
                    │                                  │
                    │                                  ▼
                    │                         ArticleEventConsumer
                    │                                  │
                    │                                  ▼
                    │                       ArticleProcessingService
                    │                                  │
                    │             ┌────────────────────┼──────────────────┐
                    │             │                    │                  │
                    │             ▼                    ▼                  ▼
                    │        Enrichment             /embed          Metadata
                    │                                  │
                    │                                  ▼
                    │                           384-d embedding
                    │                                  │
                    │                                  ▼
                    │                             pgvector
                    │                                  │
                    │                                  ▼
                    │                         Candidate retrieval
                    │                                  │
                    │                                  ▼
                    │                             /predict
                    │                                  │
                    │                                  ▼
                    │                          Event assignment
                    │
                    └──────────────────────────────────┐
                                                       │
                                                       ▼
React/Vite ─────── JWT ───────> Spring Boot REST API
                                      │
                                      ▼
                              Gemini Event Analysis
```

------------------------------------------------------------------------

# End-to-End Data Flow

## 1. Article collection

A configured news source is fetched by the Spring Boot collection layer.

``` text
NewsSource
    ↓
NewsSourceClient
    ↓
CollectedArticle
```

The service checks:

-   whether the source exists
-   whether it is enabled
-   whether the article URL already exists

Duplicate URLs are ignored.

------------------------------------------------------------------------

## 2. Article persistence

A new article is saved in PostgreSQL.

The database generates the article ID.

``` text
Create Article
      ↓
PostgreSQL
      ↓
Generated Article ID
```

The generated ID is then included in the Kafka event.

------------------------------------------------------------------------

## 3. Kafka publication

The backend publishes an `ArticleEvent` to:

``` text
xnews.articles
```

The processing consumer belongs to:

``` text
xnews-processing
```

------------------------------------------------------------------------

## 4. Article processing

The Kafka consumer loads the article and runs the enrichment pipeline.

``` text
Article
  ↓
Clean description
  ↓
Category
  ↓
Keywords
  ↓
Sentiment
  ↓
Embedding
  ↓
Vector storage
  ↓
Candidate retrieval
  ↓
Event matching
  ↓
Create or attach to event
```

------------------------------------------------------------------------

# Semantic Event Matching

This is the core intelligence upgrade introduced in V2.

## Why semantic matching?

Keyword matching is not enough.

Consider:

``` text
"India launches a national artificial intelligence initiative"

"Government announces a new AI programme across India"
```

The wording is different, but the underlying meaning can be highly
similar.

Sentence embeddings convert each article into a dense numerical
representation.

``` text
Article
   ↓
Sentence Transformer
   ↓
384-dimensional vector
```

X-NEWS currently uses:

``` text
sentence-transformers/all-MiniLM-L6-v2
```

The resulting vectors are normalized.

------------------------------------------------------------------------

# Two-Stage Event Matching

X-NEWS does not compare every article against every event.

Instead:

``` text
             Incoming Article
                    │
                    ▼
             384-d embedding
                    │
                    ▼
              pgvector search
                    │
                    ▼
          Top candidate articles
                    │
                    ▼
            Candidate events
                    │
                    ▼
          Learned event matcher
                    │
                    ▼
             SAME_EVENT?
              /       \
            YES        NO
             │          │
             ▼          ▼
        Attach to    Create new
         event         event
```

This significantly reduces the search space.

------------------------------------------------------------------------

# Final Event-Matching Model

The initial experiment used four features:

``` text
1. similarity
2. entity_score
3. temporal_score
4. location_score
```

The learned coefficients showed that semantic similarity and temporal
compatibility carried the strongest signal.

Entity and location information can also be unavailable or unreliable
for some articles.

Therefore, the production experiment was simplified to:

``` text
similarity
temporal_score
```

The final model is:

``` text
StandardScaler
      ↓
LogisticRegression
```

The trained artifact is:

``` text
event_matcher.pkl
```

------------------------------------------------------------------------

# Temporal Scoring

The current deterministic temporal compatibility function is:

  Date Difference       Score
  ------------------- -------
  Same day                1.0
  1 day                   0.9
  2--3 days               0.8
  4--7 days               0.6
  8--30 days              0.4
  More than 30 days       0.1
  Missing date            0.5

The logistic regression learns how strongly this temporal signal should
influence event matching.

------------------------------------------------------------------------

# AI Microservice

The AI service is implemented separately using Python and FastAPI.

Its purpose is to isolate machine-learning inference from the Java
application.

``` text
Spring Boot
     │
     │ HTTP
     ▼
FastAPI
     │
     ├── /embed
     │
     └── /predict
```

## Why a separate AI service?

Python provides the natural ecosystem for:

-   sentence-transformers
-   scikit-learn
-   NumPy
-   pandas
-   model serialization

The Spring Boot application remains responsible for:

-   business logic
-   persistence
-   Kafka
-   authentication
-   API orchestration

This creates a clean service boundary.

------------------------------------------------------------------------

# FastAPI Endpoints

## Health

``` http
GET /health
```

Response:

``` json
{
  "status": "healthy"
}
```

------------------------------------------------------------------------

## Embedding

``` http
POST /embed
```

Request:

``` json
{
  "text": "India launches a new artificial intelligence initiative"
}
```

The service returns a normalized 384-dimensional embedding.

The embedding is stored internally in PostgreSQL and is not exposed in
normal article API responses.

------------------------------------------------------------------------

## Event Prediction

``` http
POST /predict
```

This endpoint uses the trained event-matching model to determine the
probability that two articles belong to the same event.

The final production feature set is:

``` text
similarity
temporal_score
```

------------------------------------------------------------------------

# Kafka Processing

Kafka provides the asynchronous processing boundary.

``` text
Spring Boot Producer
        │
        ▼
xnews.articles
        │
        ▼
Kafka Consumer Group
xnews-processing
        │
        ▼
ArticleProcessingService
```

## Why Kafka?

Kafka provides:

-   asynchronous processing
-   producer/consumer decoupling
-   independent scaling
-   replay potential
-   a foundation for future processing pipelines

The collector does not have to perform all AI and event matching work
inside the original HTTP request.

------------------------------------------------------------------------

# Idempotent Processing

Article processing checks whether an article has already been processed.

Conceptually:

``` text
if article.isProcessed():
    skip
```

This protects the system against repeated processing.

------------------------------------------------------------------------

# Backend

X-NEWS V2 uses:

``` text
Java
Spring Boot
Spring Data JPA
Hibernate
Spring Security
JWT
PostgreSQL
HikariCP
Kafka
```

## Main responsibilities

The backend manages:

-   authentication
-   authorization
-   articles
-   news sources
-   events
-   article collection
-   Kafka publication
-   article processing
-   embeddings
-   vector retrieval
-   event assignment
-   Gemini analysis

------------------------------------------------------------------------

# Frontend

The frontend is built using:

``` text
React
Vite
JavaScript
CSS
```

The frontend provides:

-   registration
-   login
-   logout
-   authenticated session handling
-   latest events
-   event detail pages
-   AI event analysis
-   loading/error states
-   event counts
-   authenticated API requests

The frontend uses:

``` text
VITE_API_URL
```

for backend configuration rather than hard-coding the API URL.

------------------------------------------------------------------------

# Authentication and Authorization

X-NEWS uses JWT-based authentication.

``` text
Register/Login
      ↓
JWT
      ↓
Frontend
      ↓
Authorization: Bearer <JWT>
      ↓
JwtAuthenticationFilter
      ↓
CustomUserDetailsService
      ↓
Spring Security
```

## Roles

There are currently two roles:

``` java
USER
ADMIN
```

Spring Security converts them into:

``` text
ROLE_USER
ROLE_ADMIN
```

------------------------------------------------------------------------

# Intended Permission Model

  Capability              USER   ADMIN
  ---------------------- ------ -------
  Register                 ✅     ✅
  Login                    ✅     ✅
  View current user        ✅     ✅
  View articles            ✅     ✅
  Create articles          ❌     ✅
  Delete articles          ❌     ✅
  View sources             ✅     ✅
  Create sources           ❌     ✅
  Modify sources           ❌     ✅
  Collect from sources     ❌     ✅
  View events              ✅     ✅
  Analyze events           ✅     ✅
  Fact-check events        ✅     ✅
  Delete events            ❌     ✅

The backend is the actual security boundary. Frontend UI restrictions
are only a usability layer.

------------------------------------------------------------------------

# Database and Vector Search

PostgreSQL is the persistent source of application state.

JPA/Hibernate provides ORM mapping.

HikariCP maintains a pool of database connections so application threads
can reuse database connections rather than repeatedly establishing new
connections.

------------------------------------------------------------------------

## Article Embeddings

The selected embedding model produces:

``` text
384 dimensions
```

The database stores the vector using pgvector:

``` text
vector(384)
```

The embedding is an internal processing artifact and is not included in
normal article JSON responses.

------------------------------------------------------------------------

# pgvector Candidate Retrieval

For an incoming article:

``` text
embedding
   ↓
pgvector nearest-neighbor search
   ↓
top 30 article candidates
   ↓
candidate events
```

The current processing implementation retrieves up to 30 nearest
articles.

This is important for scalability.

Without candidate retrieval:

``` text
new article
    ↓
compare against every event
```

With candidate retrieval:

``` text
new article
    ↓
vector search
    ↓
small candidate set
    ↓
learned matcher
```

------------------------------------------------------------------------

# Gemini Event Analysis

Semantic event matching answers:

> "Which event does this article belong to?"

Gemini event analysis answers a different question:

> "What should the user understand about this event?"

The current flow is:

``` text
React
  ↓
POST /api/v1/events/{id}/analyze
  ↓
Spring Boot
  ↓
Gemini
  ↓
Analysis
  ↓
React
```

The Gemini layer should not be considered a fact-verification oracle.
Evidence-grounded analysis and RAG are intentionally reserved for V3.

------------------------------------------------------------------------

# Technology Stack

  Layer                Technology
  -------------------- ----------------------------------
  Frontend             React + Vite
  Backend              Java + Spring Boot
  ORM                  Hibernate / Spring Data JPA
  Database             PostgreSQL
  Connection Pool      HikariCP
  Vector Search        pgvector
  Messaging            Apache Kafka / Confluent Cloud
  AI API               FastAPI
  Embeddings           Sentence Transformers
  Embedding Model      all-MiniLM-L6-v2
  ML Classifier        scikit-learn Logistic Regression
  Generative AI        Gemini
  Authentication       JWT
  Password Hashing     BCrypt
  Containerization     Docker
  Backend Cloud        Azure Container Apps
  Container Registry   Azure Container Registry
  Frontend Cloud       Azure Static Web Apps

------------------------------------------------------------------------

# Project Structure

The repository is organized around the major system components.

A representative structure is:

``` text
X News/
│
├── backend/
│   └── Spring Boot application
│       ├── auth
│       ├── security
│       ├── news
│       ├── processing
│       ├── event
│       ├── kafka
│       └── AI clients
│
├── frontend/
│   └── React + Vite application
│       ├── components
│       ├── pages
│       ├── context
│       ├── services
│       └── styles
│
├── ai/
│   └── FastAPI service
│       ├── app.py
│       ├── predict.py
│       └── models/
│
├── semantic-experiment/
│   ├── data/
│   ├── models/
│   └── training scripts
│
└── .github/
    └── workflows/
```

> Exact directory names can differ depending on the current repository
> layout.

------------------------------------------------------------------------

# Running X-NEWS Locally

## Prerequisites

Install:

-   Java 21+
-   Maven or Maven Wrapper
-   Node.js + npm
-   Python 3.10+
-   PostgreSQL
-   pgvector
-   Kafka / Confluent Cloud credentials
-   Gemini API credentials if event analysis is enabled

------------------------------------------------------------------------

## 1. Start PostgreSQL

Ensure PostgreSQL is running and the database is available.

The database must support pgvector.

Verify the vector extension:

``` sql
CREATE EXTENSION IF NOT EXISTS vector;
```

------------------------------------------------------------------------

## 2. Configure Spring Boot

Configure the required application properties/environment variables for:

``` text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

KAFKA_BOOTSTRAP_SERVERS
KAFKA_API_KEY
KAFKA_API_SECRET

AI_SERVICE_URL

GEMINI_API_KEY
```

Use your project's actual property names if they differ.

Never commit secrets to GitHub.

------------------------------------------------------------------------

## 3. Start the AI service

From the AI service directory:

``` bash
python -m venv .venv
```

Activate the environment.

Windows:

``` powershell
.venv\Scripts\activate
```

Install dependencies:

``` bash
pip install -r requirements.txt
```

Start FastAPI:

``` bash
uvicorn app:app --reload
```

Verify:

``` http
GET http://localhost:8000/health
```

Expected:

``` json
{
  "status": "healthy"
}
```

------------------------------------------------------------------------

## 4. Start Spring Boot

From the backend:

``` bash
./mvnw spring-boot:run
```

On Windows:

``` powershell
.\mvnw.cmd spring-boot:run
```

The API is typically available at:

``` text
http://localhost:8080
```

------------------------------------------------------------------------

## 5. Start the frontend

From:

``` text
frontend/
```

install dependencies:

``` bash
npm install
```

Run:

``` bash
npm run dev
```

Vite normally starts the frontend on:

``` text
http://localhost:5173
```

Configure:

``` text
VITE_API_URL=http://localhost:8080/api/v1
```

------------------------------------------------------------------------

# Environment Variables

A typical local configuration requires values equivalent to:

``` env
# Spring Boot
DATABASE_URL=...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...

# Kafka
KAFKA_BOOTSTRAP_SERVERS=...
KAFKA_API_KEY=...
KAFKA_API_SECRET=...

# AI service
AI_SERVICE_URL=http://localhost:8000

# Gemini
GEMINI_API_KEY=...

# Frontend
VITE_API_URL=http://localhost:8080/api/v1
```

The exact environment/property names must match the application's
configuration.

### Never commit:

``` text
.env
API keys
JWT secrets
Kafka credentials
Database passwords
Gemini credentials
```

------------------------------------------------------------------------

# API Overview

## Authentication

``` http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/users/me
```

------------------------------------------------------------------------

## Articles

``` http
GET    /api/v1/articles
GET    /api/v1/articles/{id}
POST   /api/v1/articles
DELETE /api/v1/articles/{id}
```

------------------------------------------------------------------------

## Sources

``` http
GET   /api/v1/sources
GET   /api/v1/sources/enabled
GET   /api/v1/sources/{id}

POST  /api/v1/sources
GET   /api/v1/sources/{id}/fetch
POST  /api/v1/sources/{id}/collect

PATCH /api/v1/sources/{id}/enable
PATCH /api/v1/sources/{id}/disable
```

------------------------------------------------------------------------

## Events

``` http
GET    /api/v1/events
GET    /api/v1/events/{id}
DELETE /api/v1/events/{id}
```

------------------------------------------------------------------------

## AI Event Analysis

``` http
POST /api/v1/events/{eventId}/analyze
```

------------------------------------------------------------------------

## Internal AI Service

``` http
GET  /health
POST /embed
POST /predict
```

------------------------------------------------------------------------

# Semantic Experiment

The semantic experiment was designed to answer a specific question:

> Can semantic similarity and temporal compatibility accurately
> determine whether two news articles describe the same real-world
> event?

------------------------------------------------------------------------

## Dataset

The synthetic dataset was designed around event-aware article pairs.

The reported dataset statistics were:

  Metric                       Value
  -------------------------- -------
  Labeled pairs                1,000
  Event groups                   378
  Training samples               811
  Testing samples                189
  Training SAME_EVENT            403
  Training DIFFERENT_EVENT       408
  Testing SAME_EVENT              97
  Testing DIFFERENT_EVENT         92

The dataset intentionally includes difficult cases such as:

-   paraphrased reporting
-   different sources
-   same entities but different incidents
-   follow-up reporting
-   breaking-news updates

------------------------------------------------------------------------

# Event-Aware Train/Test Split

A naive random row split can leak information.

If two pairs describe the same event, putting one in training and
another in testing can make the model appear better than it actually is.

X-NEWS therefore constructs event groups using Union-Find and uses:

``` text
GroupShuffleSplit
test_size = 0.20
random_state = 42
```

This produces a more meaningful evaluation.

------------------------------------------------------------------------

# Four-Feature Experiment

The original model used:

``` text
similarity
entity_score
temporal_score
location_score
```

Reported metrics:

  Metric        Result
  ----------- --------
  Accuracy       0.847
  Precision      0.905
  Recall         0.784
  F1             0.840

The learned coefficients were approximately:

``` text
similarity       +0.7623
entity_score     +0.2412
temporal_score   +1.8010
location_score   +0.4613
intercept        +0.1265
```

------------------------------------------------------------------------

# Final Two-Feature Model

Because entity and location signals were less reliable and the
experiment showed that semantic similarity and temporal compatibility
were the strongest signals, the production model was simplified to:

``` text
similarity
temporal_score
```

Final reported two-feature run:

  Metric        Result
  ----------- --------
  Accuracy       0.857
  Precision      0.938
  Recall         0.773
  F1             0.847

Reported confusion matrix:

``` text
[[87, 5],
 [22, 75]]
```

Final learned coefficients:

``` text
similarity       +0.8633
temporal_score   +1.8298
intercept        +0.0787
```

The model is intentionally small and interpretable.

------------------------------------------------------------------------

# Model Training

The semantic experiment follows:

``` text
Dataset
   ↓
Sentence Transformer
   ↓
384-d embeddings
   ↓
Feature extraction
   ↓
Event-aware grouping
   ↓
GroupShuffleSplit
   ↓
StandardScaler
   ↓
LogisticRegression
   ↓
Evaluation
   ↓
event_matcher.pkl
```

A representative reproduction flow:

``` bash
python main2_2features.py
```

The exact script name/path should match the repository's current
semantic-experiment directory.

------------------------------------------------------------------------

# V1 vs V2

## V1

The first version established the core platform:

-   Spring Boot backend
-   PostgreSQL
-   Kafka
-   Docker
-   Azure deployment
-   React frontend
-   authentication infrastructure
-   initial article/event architecture

V1 was deployed to the cloud and provided the production foundation.

------------------------------------------------------------------------

## V2

V2 turns the platform into an event-intelligence system.

Major additions include:

``` text
Semantic embeddings
       ↓
pgvector
       ↓
Candidate retrieval
       ↓
Learned event matching
       ↓
Better event clustering
       ↓
AI event analysis
       ↓
Frontend authentication integration
```

V2 was merged into `master` and the repository was verified clean and
synchronized.

------------------------------------------------------------------------

# Known Limitations

V2 is intentionally not the final version.

## 1. Representative article

Event matching currently uses a representative article from the
candidate event.

Future versions can use:

-   event centroid embeddings
-   multiple representative articles
-   event-level embeddings

------------------------------------------------------------------------

## 2. Article-level vector retrieval

The current vector search retrieves article candidates and then maps
them to events.

A future architecture can retrieve event-level vectors directly.

------------------------------------------------------------------------

## 3. Limited learned feature set

The final model intentionally uses only:

``` text
similarity
temporal_score
```

Entity and location features were removed because they are not
consistently reliable.

They may return in a future richer model after better extraction and
evaluation.

------------------------------------------------------------------------

## 4. AI analysis is not evidence verification

Gemini-generated analysis is not equivalent to verified truth.

Without evidence retrieval, the model can still:

-   misunderstand a source
-   hallucinate
-   overgeneralize
-   confidently state unsupported claims

This is why evidence-grounded analysis is a V3 priority.

------------------------------------------------------------------------

## 5. Kafka production hardening

Future work includes:

-   retry topics
-   dead-letter queues
-   stronger idempotency
-   schema evolution
-   consumer lag monitoring
-   replay procedures
-   observability

------------------------------------------------------------------------

# V3 Roadmap

The next major evolution is from:

> **Event intelligence**

toward:

> **Evidence-grounded news intelligence**

Planned capabilities:

``` text
RAG
 ↓
Evidence retrieval
 ↓
Claim extraction
 ↓
Evidence ranking
 ↓
Source agreement
 ↓
Source independence
 ↓
Conflict detection
 ↓
Event timeline
 ↓
"What Changed?"
 ↓
Grounded AI analysis
```

------------------------------------------------------------------------

## Proposed V3 Architecture

``` text
                    News Sources
                         │
                         ▼
                     Collectors
                         │
                         ▼
                       Kafka
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
      Enrichment      Embeddings     Deduplication
          │              │              │
          └──────────────┼──────────────┘
                         ▼
                 Event Matching
                         │
                         ▼
                     Events
                         │
          ┌──────────────┼───────────────┐
          ▼              ▼               ▼
       Claims         Evidence        Timeline
       Extractor      Retrieval       Builder
          │              │               │
          └──────────────┼───────────────┘
                         ▼
                   Grounded AI
                         │
                         ▼
                    React UI
```

The goal is not simply to add more AI.

The goal is to make the AI's conclusions **traceable to evidence**.

------------------------------------------------------------------------

# Engineering Lessons

X-NEWS V2 also demonstrates several important engineering principles.

### Microservice contracts matter

The Spring Boot client, FastAPI request model and Python inference
function must evolve together.

A mismatch such as:

``` text
Java request
      ≠
FastAPI schema
      ≠
Python function signature
```

immediately creates runtime failures.

------------------------------------------------------------------------

### Build-time frontend configuration matters

Vite environment variables are injected during the build.

Therefore:

``` text
VITE_API_URL
```

must be available to the frontend build process.

Setting a backend environment variable after the frontend has already
been built does not automatically update the generated JavaScript.

------------------------------------------------------------------------

### Store embeddings, don't repeatedly regenerate them

Embedding generation is model inference.

Once an article's embedding has been generated:

``` text
Generate once
    ↓
Persist
    ↓
Reuse
```

This reduces unnecessary AI inference and makes vector retrieval
efficient.

------------------------------------------------------------------------

### Retrieval before expensive matching

The system should not compare a new article against every event.

Instead:

``` text
Vector retrieval
     ↓
Small candidate set
     ↓
Learned matcher
```

This is both faster and easier to scale.

------------------------------------------------------------------------

### Backend security is the real security boundary

The frontend can hide admin controls, but that is not security.

The backend must enforce:

``` text
USER
  ↓
allowed operations

ADMIN
  ↓
additional administrative operations
```

------------------------------------------------------------------------

# Why X-NEWS V2 Matters

X-NEWS V2 is not just a CRUD news application.

It combines:

``` text
Distributed Systems
        +
Backend Engineering
        +
Event-Driven Architecture
        +
Machine Learning
        +
Natural Language Processing
        +
Vector Search
        +
Generative AI
        +
Authentication
        +
Cloud Infrastructure
        +
Frontend Engineering
```

The most important architectural decision is the separation of
responsibilities:

``` text
PostgreSQL
    → persistent state

Kafka
    → asynchronous processing

Sentence Transformers
    → semantic representation

pgvector
    → candidate retrieval

Logistic Regression
    → event-match decision

Gemini
    → generative event analysis

Spring Boot
    → application orchestration

React
    → user experience
```

This makes each component replaceable and gives the system a strong
foundation for V3.

------------------------------------------------------------------------

# Current Release

## X-NEWS V2.0

Status:

``` text
Backend                  ✅
Frontend                 ✅
Authentication           ✅
Authorization foundation ✅
Kafka processing         ✅
Embeddings               ✅
pgvector                 ✅
Semantic event matching  ✅
AI microservice          ✅
Gemini analysis          ✅
Local end-to-end flow    ✅
V2 merged into master    ✅
```

V3 capabilities such as RAG, evidence grounding, claim verification,
conflict detection and timelines remain future work.

------------------------------------------------------------------------

# Contributing

The project is currently developed as a focused engineering project.

When contributing:

1.  Keep business logic in services rather than controllers.
2.  Keep DTOs explicit at API boundaries.
3.  Do not expose embeddings through public API responses.
4.  Keep AI inference isolated behind service boundaries.
5.  Preserve Kafka message contracts.
6.  Do not commit secrets.
7.  Add tests when changing authorization rules.
8.  Evaluate ML changes using event-aware splits.
9.  Avoid claiming AI-generated information is verified without
    evidence.
10. Document architectural changes.

------------------------------------------------------------------------

# Security

If you discover a security issue, do not open a public issue containing
credentials, tokens, private information, or an exploitable proof of
concept.

Remove all secrets from local files before committing:

``` text
.env
application-local.properties
API keys
JWT secrets
Kafka credentials
database credentials
```

------------------------------------------------------------------------

# License

Add the project's chosen license here before making the repository
public.

For example:

``` text
MIT License
```

or another license appropriate for the project.

------------------------------------------------------------------------

# Final Architecture at a Glance

``` text
                         X-NEWS V2
                             │
                 ┌───────────┴───────────┐
                 │                       │
             FRONTEND                 BACKEND
             React/Vite              Spring Boot
                 │                       │
                 │                       ├── JWT Security
                 │                       ├── REST APIs
                 │                       ├── PostgreSQL
                 │                       ├── Kafka
                 │                       └── Event Processing
                 │
                 └───────────┬───────────┘
                             │
                             ▼
                       AI PIPELINE
                             │
                  ┌──────────┴──────────┐
                  │                     │
             Sentence                Gemini
             Transformer             Analysis
                  │
                  ▼
             384-d vector
                  │
                  ▼
               pgvector
                  │
                  ▼
          Candidate Event Retrieval
                  │
                  ▼
          Logistic Regression
                  │
                  ▼
            Event Assignment
                  │
                  ▼
             X-NEWS EVENT
```

**X-NEWS V2 --- from articles to events, from events to intelligence.**
