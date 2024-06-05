# Procurement Forecasting and Planning Service

This service leverages past procurement data and accounting information to:
- Visualize statistics
- Forecast procurement needs
- Prepare data for procurement planning

The goal of the service is to reduce the labor required by government institutions for procurement planning by automating the forecasting of product needs and their quantities.

## Features

- **Chat-Bot Interface**: Interact with the service through a user-friendly chat-bot.
- **Data Consolidation**: Integrates past procurement and accounting data.
- **Forecasting**: Uses machine learning models to predict future procurement needs.
- **Visualization**: Provides graphical representations and reports of procurement data.

## Architecture

The architecture consists of the following key components:

1. **User Interface (UI)**
   - Chat-bot integrated with popular messengers (e.g., Telegram, WhatsApp) or a web interface.
   
2. **Dialog Management Service**
   - Handles user requests and manages dialog flows using NLP.
   
3. **Data Processing Service**
   - Consolidates, cleans, validates, and pre-processes procurement data.
   
4. **Analytics and Forecasting Service**
   - Analyzes historical data and uses ML models for forecasting.
   
5. **Data Visualization Service**
   - Creates charts and reports for data presentation.
   
6. **Database**
   - Stores procurement data, accounting data, and analysis results using both relational (PostgreSQL) and NoSQL (MongoDB) databases.
   
7. **Authentication and Authorization Service**
   - Manages user access to different service functions.

## Technology Stack

- **Chat-Bot and Interface**: Node.js, Telegraf (for Telegram), React.js
- **Data Processing and Analysis**: Python, pandas, scikit-learn
- **Database**: PostgreSQL, MongoDB
- **Visualization**: D3.js, Plotly, Dash
- **Infrastructure and Deployment**: Docker, Kubernetes, CI/CD (Jenkins, GitLab CI)

## Getting Started

### Prerequisites

- Docker
- Node.js
- Python 3.8+
- PostgreSQL
- MongoDB

### Installation

1. **Clone the repository**:
    ```sh
    git clone https://github.com/yourusername/procurement-forecasting-service.git
    cd procurement-forecasting-service
    ```

2. **Set up the environment**:
    - Create a `.env` file and populate it with the necessary environment variables.
    - Example:
      ```env
      DATABASE_URL=postgresql://username:password@localhost:5432/procurement_db
      MONGO_URL=mongodb://localhost:27017
      ```

3. **Build and run the services using Docker**:
    ```sh
    docker-compose up --build
    ```

### Usage

1. **Access the chat-bot**:
   - Depending on the integration, access the chat-bot through the chosen messenger or web interface.

2. **Interact with the chat-bot**:
   - Use natural language to request procurement data, forecasts, and visualizations.

## Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -am 'Add new feature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Create a new Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

