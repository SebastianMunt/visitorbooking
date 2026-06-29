function initCalendar(calendarElement) {
    const startDateInput = document.querySelector('input[name="startDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');
    const calendarApiUrl = calendarElement.dataset.apiUrl;
    const footballToggle = document.getElementById("football-toggle");

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

        eventClassNames: function (info) {
            if (isFootballEvent(info.event)) {
                return ["football-calendar-event"];
            }

            return [];
        },

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

            updateTripPreview(startDateInput, endDateInput);
        },

        select: function (info) {
            if (!startDateInput || !endDateInput) {
                return;
            }

            hideEventBubble();

            startDateInput.value = info.startStr;
            endDateInput.value = subtractOneDay(info.endStr);

            mobileStartDate = null;
            updateTripPreview(startDateInput, endDateInput);
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
                    const visibleEvents = filterFootballEvents(events);

                    successCallback(visibleEvents);

                    setTimeout(function () {
                        renderNextAvailableWeekend(events);
                        renderCalendarHeatmap(visibleEvents);
                    }, 100);
                })
                .catch(error => failureCallback(error));
        }
    });

    calendar.render();

    if (footballToggle) {
        footballToggle.addEventListener("change", function () {
            calendar.refetchEvents();
        });
    }

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
        startDateInput.addEventListener("change", function () {
            updateTripPreview(startDateInput, endDateInput);
        });

        endDateInput.addEventListener("change", function () {
            updateTripPreview(startDateInput, endDateInput);
        });
    }

    function handleMobileDateClick(clickedDate) {
        if (mobileStartDate === null) {
            mobileStartDate = clickedDate;
            startDateInput.value = clickedDate;
            endDateInput.value = clickedDate;
            updateTripPreview(startDateInput, endDateInput);
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
        updateTripPreview(startDateInput, endDateInput);
    }
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

    const returnDate = new Date(2027, 0, 27);

    html += `
        <div class="next-info-block next-return-block">
            <span class="next-info-label">Hjemrejsedato</span>
            <strong>${formatFullPrettyDate(returnDate)}</strong>
            <span>Planlagt hjemrejse fra Barcelona.</span>
        </div>
    `;

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
    return event.color === "#28a745" ||
        event.color === "#1d4ed8" ||
        isFootballEvent(event);
}

function updateTripPreview(startDateInput, endDateInput) {
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

function filterFootballEvents(events) {
    const footballToggle = document.getElementById("football-toggle");

    if (!footballToggle || footballToggle.checked) {
        return events;
    }

    return events.filter(event => !isFootballEvent(event));
}

function isFootballEvent(event) {
    return event.title === "⚽️" ||
        event.bookingType === "Fodboldkamp" ||
        event.extendedProps?.bookingType === "Fodboldkamp";
}