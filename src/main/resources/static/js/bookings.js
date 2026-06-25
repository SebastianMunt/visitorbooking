document.addEventListener("DOMContentLoaded", function () {
    const calendarElement = document.getElementById("calendar");
    setupBookingSuccessPopup();

    if (!calendarElement) {
        loadWeather();
        return;
    }

    const startDateInput = document.querySelector('input[name="startDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');
    const calendarApiUrl = calendarElement.dataset.apiUrl;

    let mobileStartDate = null;

    createEventBubble();

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

            hideEventBubble();

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

            hideEventBubble();

            startDateInput.value = info.startStr;
            endDateInput.value = subtractOneDay(info.endStr);

            mobileStartDate = null;
            updateTripPreview();
        },

        eventClick: function (info) {
            info.jsEvent.preventDefault();
            info.jsEvent.stopPropagation();

            const title = info.event.title || "Event";
            const dateText = getEventDateText(info.event);

            const type = info.event.extendedProps.bookingType;
            const comment = info.event.extendedProps.comment;
            const days = info.event.extendedProps.days;

            showEventBubble(title, dateText, info.jsEvent, type, comment, days);
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

    document.addEventListener("click", function (event) {
        const bubble = document.getElementById("event-message-bubble");

        if (!bubble) {
            return;
        }

        if (!bubble.contains(event.target) && !event.target.closest(".fc-event")) {
            hideEventBubble();
        }
    });

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

    function createEventBubble() {
        if (document.getElementById("event-message-bubble")) {
            return;
        }

        const bubble = document.createElement("div");
        bubble.id = "event-message-bubble";

        bubble.innerHTML = `
            <div id="event-message-bubble-title"></div>
            <div id="event-message-bubble-date"></div>
        `;

        bubble.style.position = "fixed";
        bubble.style.zIndex = "9999";
        bubble.style.maxWidth = "min(280px, calc(100vw - 32px))";
        bubble.style.padding = "12px 14px";
        bubble.style.borderRadius = "22px";
        bubble.style.background = "#28a745";
        bubble.style.color = "#ffffff";
        bubble.style.boxShadow = "0 12px 30px rgba(0, 0, 0, 0.22)";
        bubble.style.fontFamily = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif';
        bubble.style.fontSize = "0.95rem";
        bubble.style.lineHeight = "1.25";
        bubble.style.wordBreak = "break-word";
        bubble.style.opacity = "0";
        bubble.style.pointerEvents = "none";
        bubble.style.transform = "translateY(8px) scale(0.96)";
        bubble.style.transition = "opacity 160ms ease, transform 160ms ease";
        bubble.style.display = "none";

        document.body.appendChild(bubble);

        const titleElement = document.getElementById("event-message-bubble-title");
        const dateElement = document.getElementById("event-message-bubble-date");

        titleElement.style.fontWeight = "800";
        titleElement.style.marginBottom = "4px";

        dateElement.style.fontSize = "0.78rem";
        dateElement.style.opacity = "0.88";
        dateElement.style.fontWeight = "650";
    }

    function showEventBubble(title, dateText, mouseEvent, type, comment, days) {
        const bubble = document.getElementById("event-message-bubble");
        const titleElement = document.getElementById("event-message-bubble-title");
        const dateElement = document.getElementById("event-message-bubble-date");

        if (!bubble || !titleElement || !dateElement) {
            return;
        }

        titleElement.textContent = title;

        let details = dateText;

        if (days) {
            details += ` · ${days} dag${days > 1 ? "e" : ""}`;
        }

        if (type) {
            details += ` · ${type}`;
        }

        if (comment && comment.trim() !== "") {
            details += `\n${comment}`;
        }

        dateElement.textContent = details;
        dateElement.style.whiteSpace = "pre-line";

        bubble.style.display = "block";
        bubble.style.opacity = "0";
        bubble.style.pointerEvents = "auto";
        bubble.style.transform = "translateY(8px) scale(0.96)";

        const bubbleWidth = Math.min(300, window.innerWidth - 32);
        let left = mouseEvent.clientX - bubbleWidth / 2;
        let top = mouseEvent.clientY - 92;

        if (left < 16) {
            left = 16;
        }

        if (left + bubbleWidth > window.innerWidth - 16) {
            left = window.innerWidth - bubbleWidth - 16;
        }

        if (top < 84) {
            top = mouseEvent.clientY + 18;
        }

        bubble.style.width = bubbleWidth + "px";
        bubble.style.left = left + "px";
        bubble.style.top = top + "px";

        requestAnimationFrame(function () {
            bubble.style.opacity = "1";
            bubble.style.transform = "translateY(0) scale(1)";
        });
    }

    function hideEventBubble() {
        const bubble = document.getElementById("event-message-bubble");

        if (!bubble) {
            return;
        }

        bubble.style.opacity = "0";
        bubble.style.pointerEvents = "none";
        bubble.style.transform = "translateY(8px) scale(0.96)";

        setTimeout(function () {
            if (bubble.style.opacity === "0") {
                bubble.style.display = "none";
            }
        }, 180);
    }

    function getEventDateText(event) {
        const startText = formatPrettyDate(event.start);

        if (!event.end) {
            return startText;
        }

        const realEndDate = subtractOneDay(formatDate(event.end));
        const endText = formatPrettyDate(realEndDate);

        if (endText === startText) {
            return startText;
        }

        return `${startText} til ${endText}`;
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
                    <span>Næste weekend der ser helt ledig ud efter 1/9.</span>
                </div>
            `;
        } else {
            html += `
                <div class="next-info-block">
                    <span class="next-info-label">Næste ledige weekend</span>
                    <strong>Ingen ledige weekender fundet</strong>
                    <span>Der er ikke fundet en ledig weekend efter 1/9.</span>
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

        let date = new Date();
        const minimumDate = new Date(2026, 8, 1);

        if (date < minimumDate) {
            date = minimumDate;
        }

        date.setHours(0, 0, 0, 0);

        while (date.getDay() !== 5) {
            date.setDate(date.getDate() + 1);
        }

        for (let i = 0; i < 104; i++) {
            const friday = new Date(date);
            const saturday = addDays(friday, 1);
            const sunday = addDays(friday, 2);

            const fridayKey = formatDate(friday);
            const saturdayKey = formatDate(saturday);
            const sundayKey = formatDate(sunday);

            const weekendIsFree =
                !occupiedDates.has(fridayKey) &&
                !occupiedDates.has(saturdayKey) &&
                !occupiedDates.has(sundayKey);

            if (weekendIsFree) {
                return {
                    friday: friday,
                    saturday: saturday,
                    sunday: sunday
                };
            }

            date.setDate(date.getDate() + 7);
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
        if (dateString instanceof Date) {
            return new Date(dateString);
        }

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

    function formatPrettyDate(dateInput) {
        const date = parseDate(dateInput);

        return date.toLocaleDateString("da-DK", {
            day: "numeric",
            month: "short"
        });
    }

    function setupBookingSuccessPopup() {
        const popup = document.getElementById("booking-success-popup");

        if (!popup) {
            return;
        }

        popup.addEventListener("click", function () {
            popup.classList.add("is-hidden");

            setTimeout(function () {
                popup.remove();
            }, 200);
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