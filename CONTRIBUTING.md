# Contributing to Sender

Thank you for your interest in contributing to **Sender**! We welcome contributions to help make air-gapped data transfers faster, safer, and more resilient.

## Getting Started

1. **Fork the repository** on GitHub.
2. **Clone your fork**:
   ```bash
   git clone https://github.com/gamm3r96/sender.git
   cd sender
   ```
3. **Create a feature branch**:
   ```bash
   git checkout -b feature/amazing-feature
   ```

## Development Guidelines

- **Code Style**: We use Kotlin and Jetpack Compose following Material Design 3 guidelines.
- **Security First**: Do not bypass or weaken cryptographic guarantees (AES-256-GCM, PBKDF2 iterations, authenticated envelopes).
- **Zero Cloud Dependence**: The application must remain strictly zero-knowledge and offline-first. Never introduce third-party analytics or cloud telemetry tracking.

## Submitting Pull Requests

1. Test your changes locally to ensure clean builds.
2. Commit with clear, descriptive messages (e.g. `feat: add optical contrast preset`, `fix: fountain chunk alignment`).
3. Push to your branch and open a Pull Request against `main`.

## Contact & Author

- **Elvis Gatwara** ([@gamm3r96](https://github.com/gamm3r96))
- **Website**: [elvis-gatwara.vercel.app](https://elvis-gatwara.vercel.app)
- **Email**: [elvisgatwara@gmail.com](mailto:elvisgatwara@gmail.com)
