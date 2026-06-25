document.addEventListener('DOMContentLoaded', function () {
    const calendarElement = document.getElementById('calendar');

    if (!calendarElement) {
        return;
    }

    const startDateInput = document.querySelector('input[name="startDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');

    const calendarApiUrl = calendarElement.dataset.apiUrl;

    const calendar = new FullCalendar.Calendar(calendarElement, {
        initialView: 'dayGridMonth',
        firstDay: 1,
        locale: 'da',
        selectable: true,
        selectMirror: true,
        unselectAuto: false,

        dateClick: function (info) {
            if (startDateInput && endDateInput) {
                startDateInput.value = info.dateStr;
                endDateInput.value = info.dateStr;
            }
        },

        select: function (info) {
            if (startDateInput && endDateInput) {
                startDateInput.value = info.startStr;

                const realEndDate = subtractOneDay(info.endStr);
                endDateInput.value = realEndDate;
            }
        },

        events: async function (info, successCallback, failureCallback) {
            try {
                const response = await fetch(calendarApiUrl);
                const events = await response.json();

                successCallback(events);
            } catch (error) {
                console.error('Kunne ikke hente kalender-events:', error);
                failureCallback(error);
            }
        }
    });

    calendar.render();
});

function subtractOneDay(dateString) {
    const parts = dateString.split('-');
    const date = new Date(parts[0], parts[1] - 1, parts[2]);
    date.setDate(date.getDate() - 1);
    return formatDate(date);
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return year + '-' + month + '-' + day;
}