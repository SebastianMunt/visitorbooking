document.addEventListener("DOMContentLoaded", function () {
    const calendarElement = document.getElementById("calendar");

    if (!calendarElement) {
        loadWeather();
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

        longPressDelay: 250,
        selectLongPressDelay: 250,

        dateClick: function (info) {
            if (!startDateInput || !endDateInput) {
                return;
            }

            const clickedDate = info.dateStr;

            if (isTouchDevice()) {
                handleMobileDateClick(clickedDate);
                return;
            }

            startDateInput.value = clickedDate;
            endDateInput.value = clickedDate;

            updateTripPreview();
        },

        select: function (info) {
            if (!startDateInput || !endDateInput) {
                return;
            }

            startDateInput.value = info.startStr;
            endDateInput.value = subtractOneDay(info.endStr);

            mobileStartDate = null;
            updateTripPreview();
        },

        events: function (fetchInfo, successCallback, failureCallback) {
            fetch(calendarApiUrl)
                .then(response => response.json())
                .then(events => {
                    successCallback(events);

                    setTimeout(function () {
                        renderNextAvailableWeekend(events);
                        renderCalendarHeatmap(events);
                    }, 100);
                })
                .catch(error => failureCallback(error));
        }
    });

    calendar.render();

    loadWeather();
    loadLeaderboard();

    if (startDateInput && endDateInput) {
        startDateInput.addEventListener("change", updateTripPreview);
        endDateInput.addEventListener("change", updateTripPreview);
    }

    function handleMobileDateClick(clickedDate) {
        if (mobileStartDate === null) {
            mobileStartDate = clickedDate;
            startDateInput.value = clickedDate;
            endDateInput.value = clickedDate;
            updateTripPreview();
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
        updateTripPreview();
    }

    function renderNextAvailableWeekend(events) {
        const element = document.getElementById("next-weekend");

        if (!element) {
            return;
        }

        const nextWeekend = findNextAvailableWeekend(events);
        const nextEvent = findNextEvent(events);

        let html = "";

        if (nextWeekend) {
            html += `
            <div class="next-info-block">
                <span class="next-info-label">Næste ledige weekend</span>
                <strong>${formatPrettyDate(nextWeekend.saturday)} til ${formatPrettyDate(nextWeekend.sunday)}</strong>
                <span>Næste weekend der ser helt ledig ud.</span>
            </div>
        `;
        } else {
            html += `
            <div class="next-info-block">
                <span class="next-info-label">Ledig weekend</span>
                <span>Jeg kunne ikke finde en ledig weekend lige nu.</span>
            </div>
        `;
        }

        if (nextEvent) {
            html += `
            <div class="next-info-block next-event-block">
                <span class="next-info-label">Næste event</span>
                <strong>${nextEvent.title}</strong>
                <span>${formatPrettyDate(nextEvent.start)}${nextEvent.end ? " til " + formatPrettyDate(subtractOneDay(nextEvent.end)) : ""}</span>
            </div>
        `;
        } else {
            html += `
            <div class="next-info-block next-event-block">
                <span class="next-info-label">Næste event</span>
                <span>Der er ingen events i kalenderen endnu.</span>
            </div>
        `;
        }

        element.innerHTML = html;
    }

    function findNextAvailableWeekend(events) {
        const occupiedDates = buildOccupiedDateSet(events);
        const today = new Date();

        for (let i = 0; i < 365; i++) {
            const date = new Date(today);
            date.setDate(today.getDate() + i);

            if (date.getDay() !== 6) {
                continue;
            }

            const saturday = formatDate(date);

            const sundayDate = new Date(date);
            sundayDate.setDate(date.getDate() + 1);

            const sunday = formatDate(sundayDate);

            if (!occupiedDates.has(saturday) && !occupiedDates.has(sunday)) {
                return {
                    saturday: saturday,
                    sunday: sunday
                };
            }
        }

        return null;
    }

    function findNextEvent(events) {
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        return events
            .filter(event => isEventOnly(event))
            .filter(event => parseDate(event.start) >= today)
            .sort((a, b) => parseDate(a.start) - parseDate(b.start))[0] || null;
    }

    function renderCalendarHeatmap(events) {
        const dateCounts = {};

        events.forEach(event => {
            if (isEventOnly(event)) {
                return;
            }

            const dates = getDatesInEventRange(event);

            dates.forEach(date => {
                dateCounts[date] = (dateCounts[date] || 0) + 1;
            });
        });

        document.querySelectorAll(".fc-daygrid-day").forEach(dayElement => {
            dayElement.classList.remove("heat-low", "heat-medium", "heat-high");

            const date = dayElement.dataset.date;
            const count = dateCounts[date] || 0;

            if (count >= 2) {
                dayElement.classList.add("heat-high");
            } else if (count === 1) {
                dayElement.classList.add("heat-medium");
            }
        });
    }

    function loadLeaderboard() {
        const element = document.getElementById("guest-leaderboard");

        if (!element) {
            return;
        }

        Promise.all([
            fetch("/api/bookings/leaderboard").then(response => response.json()),
            fetch("/api/bookings/guest-highlights").then(response => response.json())
        ])
            .then(([leaderboard, highlights]) => {
                let html = "";

                if (!leaderboard || leaderboard.length === 0) {
                    html += `<p>Ingen gæster på leaderboard endnu.</p>`;
                } else {
                    html += leaderboard
                        .map((guest, index) => {
                            const bookingText = guest.visitCount === 1 ? "booking" : "bookinger";

                            return `
                            <div class="leaderboard-row">
                                <span class="leaderboard-place">${index + 1}</span>
                                <span class="leaderboard-name">${guest.guestName}</span>
                                <span class="leaderboard-count">${guest.visitCount} ${bookingText}</span>
                            </div>
                        `;
                        })
                        .join("");
                }

                html += `
                <div class="leaderboard-highlights">
                    <div class="leaderboard-highlight">
                        <span>🏆</span>
                        <p>${highlights.longestVisitText}</p>
                    </div>

                    <div class="leaderboard-highlight">
                        <span>🥇</span>
                        <p>${highlights.firstBookingText}</p>
                    </div>

                    <div class="leaderboard-highlight">
                        <span>🧳</span>
                        <p>${highlights.firstVisitText}</p>
                    </div>
                </div>
            `;

                element.innerHTML = html;
            })
            .catch(() => {
                element.textContent = "Kunne ikke hente leaderboard.";
            });
    }

    function buildOccupiedDateSet(events) {
        const occupiedDates = new Set();

        events.forEach(event => {
            if (isEventOnly(event)) {
                return;
            }

            getDatesInEventRange(event).forEach(date => occupiedDates.add(date));
        });

        return occupiedDates;
    }

    function getDatesInEventRange(event) {
        const dates = [];

        if (!event.start) {
            return dates;
        }

        const start = parseDate(event.start);
        const endExclusive = event.end ? parseDate(event.end) : addDays(start, 1);

        const current = new Date(start);

        while (current < endExclusive) {
            dates.push(formatDate(current));
            current.setDate(current.getDate() + 1);
        }

        return dates;
    }

    function isEventOnly(event) {
        return event.color === "#28a745";
    }

    function updateTripPreview() {
        const preview = document.getElementById("trip-preview");

        if (!preview || !startDateInput || !endDateInput) {
            return;
        }

        if (!startDateInput.value || !endDateInput.value) {
            preview.textContent = "Vælg datoer i kalenderen.";
            return;
        }

        const start = parseDate(startDateInput.value);
        const end = parseDate(endDateInput.value);
        const days = Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1;

        if (days <= 0) {
            preview.textContent = "Slutdato skal være efter startdato.";
            return;
        }

        preview.textContent = `${days} dag${days > 1 ? "e" : ""} valgt.`;
    }

    function isTouchDevice() {
        return window.matchMedia("(pointer: coarse)").matches;
    }

    function subtractOneDay(dateString) {
        const date = parseDate(dateString);
        date.setDate(date.getDate() - 1);

        return formatDate(date);
    }

    function parseDate(dateString) {
        const parts = dateString.split("-");
        return new Date(parts[0], parts[1] - 1, parts[2]);
    }

    function addDays(date, days) {
        const copy = new Date(date);
        copy.setDate(copy.getDate() + days);

        return copy;
    }

    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");

        return `${year}-${month}-${day}`;
    }

    function formatPrettyDate(dateString) {
        const date = parseDate(dateString);

        return date.toLocaleDateString("da-DK", {
            day: "numeric",
            month: "short"
        });
    }
});

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