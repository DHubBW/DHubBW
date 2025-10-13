<p align="center">
	<img src="./DHubBW-Logo.png" alt="DHubBW Logo" width="200" />
</p>

<h1 align="center">DHubBW – Student Platform for DHBW Karlsruhe</h1>

<p align="center"><em>One hub for files, schedules, polls, groups, and knowledge — secure, simple, and collaborative.</em></p>

<p align="center">
	<img alt="Status: Draft" src="https://img.shields.io/badge/status-draft-yellow" />
	<img alt="Scope: DHBW Karlsruhe" src="https://img.shields.io/badge/scope-DHBW%20Karlsruhe-blue" />
</p>

<p align="center"><sub>Client: DHBW Karlsruhe</sub></p>

---

## Contents

- [Overview](#overview)
- [Who it’s for](#who-its-for)
- [Key features](#key-features)
- [Tech stack](#tech-stack)
- [Collaboration](#collaboration)
- [Links](#links)
- [Future ideas](#future-ideas)

## Quick Start

For organizational and technical details see:

- [Project management](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/projektmanagement)
- [Documentation & drafts](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dokumentation)

## Overview

DHubBW brings together what students use every day — files, schedules, polls, groups and shared knowledge — in one place. It integrates existing systems (e.g., Rapla) with a student-first experience.

## Who it’s for

- Students at DHBW Karlsruhe (primary)
- Lecturers/instructors for polls and course organization (secondary)
- Start at DHBW KA, potentially extend to other campuses

## Key features

- File sharing (versions, quotas) for cheat sheets, notes, worksheets
- Calendar: timetable, deadlines, exams, canteen menu
- Polls/surveys for student decisions and feedback
- Accounts with roles & permissions (admin, course roles)
- Knowledge sharing and participation across cohorts
- Study groups and networking

## Future ideas

- Binding polls for course-level decisions
- Learning stats (e.g., who started studying on a given date)
- “Find your Buddy” matchmaking by preferences
- Basic messenger
- PDF annotations (GoodNotes-like)
- Extended admin management

## Outlook (Could-have)

- Forum features, discussion threads
- Friends/contact matching
- Multi-institution platform capability

## Requirements Engineering

- Workshops and interviews to gather and prioritize Must/Should/Could
- Continuous backlog refinement and scope alignment

## Tech stack

- Frontend: Next.js (App Router, TypeScript), React, Tailwind CSS, FullCalendar, Uppy
- Backend: Next.js API routes (REST), Supabase Auth, Postgres (RLS, FTS), Supabase Storage
- Infrastructure: Docker/Compose on VMs, Caddy/Nginx with SSL

## Notes

- Reliable Back button (UX)
- Roles/rights for admins and courses
- Lecturer rating form (concept)
- Student accounts via matriculation number; careful data handling

## Collaboration

- No force pushes to main
- Use pull/merge requests referencing issues
- Branch naming: feature/<short>, fix/<short>
- Code review required before merge

## Links

- [Project management](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/projektmanagement)
- [Documentation & drafts](https://gitlab.com/dhbw-se/se-tinf24b2/gruppe-4/dokumentation)

