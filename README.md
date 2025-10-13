# DHubBW – Student Platform for DHBW Karlsruhe

Welcome to DHubBW, the central platform for students at DHBW Karlsruhe. The goal is a single place where everything comes together: files, schedules, polls, groups, knowledge sharing, and more — secure, simple, and collaborative.

– Client: DHBW Karlsruhe

## Contents

- [Overview](#overview)
- [Target Audience and Scope](#target-audience-and-scope)
- [Core Features (Must-have)](#core-features-must-have)
- [Optional Features (Should/Nice-to-have)](#optional-features-shouldnice-to-have)
- [Outlook (Could-have)](#outlook-could-have)
- [Requirements Engineering](#requirements-engineering)
- [Technical Proposal (Tech Stack)](#technical-proposal-tech-stack)
- [Additional Notes (from discussion)](#additional-notes-from-discussion)
- [Collaboration & Workflow](#collaboration--workflow)
- [Further Content and Links](#further-content-and-links)

## Quick Start

For organizational and technical details see:

- Project Management: [../projektmanagement/README.md](../projektmanagement/README.md)
- Documentation & Drafts: [../dokumentation/README.md](../dokumentation/README.md)

## Overview

The app consolidates study-related information and tools:

- Central hub for files, schedules, polls, and groups
- Integration of existing systems (e.g., Rapla/Moodle) focused on student usage
- Learn together across cohorts, share knowledge, and connect

## Target Audience and Scope

- Primary: Students of DHBW Karlsruhe
- Secondary: Lecturers/instructors (polls, course organization)
- Start with DHBW Karlsruhe, potentially extend to other universities

## Core Features (Must-have)

- File sharing: upload/download, versions, quotas; share cheat sheets, notes, worksheets
- Calendar integration: timetable, deadlines, exam dates; canteen menu
- Polls/surveys with and for students
- Accounts: student registration and sign-in; roles & permissions (admin, course roles)
- Knowledge transfer: share posts/notes; open participation
- Networking: form study groups, connect across cohorts

## Optional Features (Should/Nice-to-have)

- Binding polls (e.g., course decisions)
- Learning statistics (e.g., “How many started studying on dd.mm.yyyy?”)
- “Find your Buddy”: matchmaking for study partners by preferences
- Online messenger (basic chat)
- Advanced file annotations: GoodNotes-like PDF markup/drawing
- Extended admin management for accounts

## Outlook (Could-have)

- Forum features, discussion threads
- Friends/contact matching
- Multi-institution platform capability

## Requirements Engineering

- Joint workshop: elicit and prioritize requirements (Must/Should/Could)
- Stakeholder interviews (students, lecturers, admins)
- Ongoing backlog management and alignment with DHBW KA scope

## Technical Proposal (Tech Stack)

Frontend
- Next.js (App Router, TypeScript) – framework & SSR
- React – UI foundation
- Tailwind CSS – styling
- FullCalendar – calendar UI
- Uppy – upload handling (resumable, drag & drop)

Backend
- Next.js API Route Handlers (REST) – business logic
- Supabase Auth – sign-in (email/OAuth)
- Postgres (Supabase) – database + Row Level Security
- Supabase Storage – file storage (private buckets)
- Postgres FTS – search in file metadata/tags

Core functions (technical)
- File sharing (upload, download, quotas, versions, shares)
- Groups & roles (owner, mod, member)
- Calendar (CRUD, participants, ICS export)
- Security (RLS, audit, GDPR-compliant)

Infrastructure
- Containerization (Docker/Compose) on VMs
- Caddy/Nginx – reverse proxy + SSL

## Additional Notes (from discussion)

- Working “Back” button (UX detail)
- Rights & roles: admins, courses, etc.
- Lecturer ratings with a form
- Accounts via matriculation number (as far as possible), secure data handling

## Collaboration & Workflow

- No force pushes to the main branch
- Changes via pull/merge requests; MRs reference related issues
- Branch naming: feature/<short-description>, fix/<short-description>
- Code reviews required before merge

## Further Content and Links

- Project Management: [../projektmanagement/README.md](../projektmanagement/README.md)
- Documentation & Drafts: [../dokumentation/README.md](../dokumentation/README.md)
- Presentation-ready concept: see documentation (collected there)

---

If you have additions or new ideas, please open an issue or create a PR with a short rationale and reference to the context (workshop/feedback, etc.).
