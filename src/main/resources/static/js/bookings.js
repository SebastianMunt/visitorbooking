document.addEventListener("DOMContentLoaded", function () {
    const calendarElement = document.getElementById("calendar");

    if (!calendarElement) {
        return;
    }

    const startDateInput = document.querySelector('input[name="startDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');
    const calendarApiUrl = calendarElement.dataset.apiUrl;

    let mobileStartDate = null;

    const calendar = new FullCalendar.Calendar(calendarElement, {
        initialView: "dayGridMonth",
        firstDay: 1,
        locale: "da",

        selectable: true,
        selectMirror: true,
        unselectAuto: false,

        // Gør touch-selection lidt bedre på mobil
        longPressDelay: 250,
        selectLongPressDelay: 250,

        dateClick: function (info) {
            if (!startDateInput || !endDateInput) {
                return;
            }

            const clickedDate = info.dateStr;

            // På mobil/tablet: første tryk vælger start, andet tryk vælger slut
            if (isTouchDevice()) {
                handleMobileDateClick(clickedDate);
                return;
            }

            // På computer: klik på én dag vælger samme start og slut
            startDateInput.value = clickedDate;
            endDateInput.value = clickedDate;
        },

        select: function (info) {
            if (!startDateInput || !endDateInput) {
                return;
            }

            // Drag-selection på computer
            startDateInput.value = info.startStr;
            endDateInput.value = subtractOneDay(info.endStr);

            mobileStartDate = null;
        },

        events: function (fetchInfo, successCallback, failureCallback) {
            fetch(calendarApiUrl)
                .then(response => response.json())
                .then(events => successCallback(events))
                .catch(error => failureCallback(error));
        }
    });

    calendar.render();

    function handleMobileDateClick(clickedDate) {
        if (mobileStartDate === null) {
            mobileStartDate = clickedDate;
            startDateInput.value = clickedDate;
            endDateInput.value = clickedDate;
            return;
        }

        if (clickedDate < mobileStartDate) {
            startDateInput.value = clickedDate;
            endDateInput.value = mobileStartDate;
        } else {
            startDateInput.value = mobileStartDate;
            endDateInput.value = clickedDate;
        }

        mobileStartDate = null;
    }

    function isTouchDevice() {
        return window.matchMedia("(pointer: coarse)").matches;
    }

    function subtractOneDay(dateString) {
        const parts = dateString.split("-");
        const date = new Date(parts[0], parts[1] - 1, parts[2]);
        date.setDate(date.getDate() - 1);

        return formatDate(date);
    }

    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");

        return `${year}-${month}-${day}`;
    }
});

loadWeather();

function loadWeather() {
    const currentElement = document.getElementById("weather-current");
    const forecastElement = document.getElementById("weather-forecast");

    if (!currentElement || !forecastElement) {
        return;
    }

    const latitude = 41.3851;
    const longitude = 2.1734;

    const url =
        `https://api.open-meteo.com/v1/forecast` +
        `?latitude=${latitude}` +
        `&longitude=${longitude}` +
        `&current=temperature_2m,weather_code` +
        `&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max` +
        `&timezone=Europe%2FMadrid` +
        `&forecast_days=6`;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            renderCurrentWeather(data, currentElement);
            renderForecast(data, forecastElement);
        })
        .catch(() => {
            currentElement.textContent = "Kunne ikke hente vejr";
        });
}

function renderCurrentWeather(data, currentElement) {
    const temperature = Math.round(data.current.temperature_2m);
    const weatherCode = data.current.weather_code;

    currentElement.innerHTML = `
        <span class="weather-icon">${getWeatherIcon(weatherCode)}</span>
        <span>${temperature}°C</span>
    `;
}

function renderForecast(data, forecastElement) {
    const daily = data.daily;

    forecastElement.innerHTML = "";

    daily.time.forEach((date, index) => {
        const dayName = formatWeatherDay(date);
        const icon = getWeatherIcon(daily.weather_code[index]);
        const maxTemp = Math.round(daily.temperature_2m_max[index]);
        const minTemp = Math.round(daily.temperature_2m_min[index]);
        const rain = daily.precipitation_probability_max[index];

        const card = document.createElement("div");
        card.className = "weather-day";
        card.innerHTML = `
            <div class="weather-day-name">${dayName}</div>
            <div class="weather-day-icon">${icon}</div>
            <div class="weather-temp">${maxTemp}° / ${minTemp}°</div>
            <div class="weather-rain">${rain}% regn</div>
        `;

        forecastElement.appendChild(card);
    });
}

function formatWeatherDay(dateString) {
    const date = new Date(dateString);

    return date.toLocaleDateString("da-DK", {
        weekday: "short"
    });
}

function getWeatherIcon(code) {
    if (code === 0) return "☀️";
    if ([1, 2].includes(code)) return "🌤️";
    if (code === 3) return "☁️";
    if ([45, 48].includes(code)) return "🌫️";
    if ([51, 53, 55, 56, 57].includes(code)) return "🌦️";
    if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return "🌧️";
    if ([71, 73, 75, 77, 85, 86].includes(code)) return "❄️";
    if ([95, 96, 99].includes(code)) return "⛈️";

    return "🌤️";
}