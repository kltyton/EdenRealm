(function () {
    var h = Vue.h;
    var bridge = document.getElementById('eden-bridge');
    var state = Vue.reactive({
        biomes: [], selected: '', profile: {}, defaults: {}, seed: 0,
        loading: true, busy: false, error: '', feedback: '', labels: {},
        previewChunks: 16, minPreviewChunks: 6
    });
    var menuOpen = Vue.ref(false);
    var chunkInput = Vue.ref('16');
    var sequence = 0;
    var pending = {};
    var commands = [];
    var posting = false;
    var polling = false;
    var lastState = '';

    function camera(message) { send('camera', message); }
    function resetCamera() {
        camera({ reset: true, sceneKey: state.generation, yaw: 0.75, angle: state.selected === 'ice_crystal_basin' ? 0.35
            : state.selected === 'icefall_fjord' ? 0.9 : 0.7 });
    }
    async function post() {
        if (posting || !commands.length) return;
        posting = true;
        var batch = commands;
        commands = [];
        try {
            var response = await fetch('/commands', { method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(batch) });
            if (!response.ok) throw new Error('Command HTTP ' + response.status);
        } catch (error) {
            state.error = error.message;
        } finally {
            posting = false;
            if (commands.length) post();
        }
    }
    async function poll() {
        if (polling) return;
        polling = true;
        try {
            var response = await fetch('/state.json', { cache: 'no-store' });
            if (response.status !== 200) return;
            var raw = await response.text();
            if (raw !== lastState) { lastState = raw; bridge.setAttribute('data-state', raw); }
        } catch (error) {
            state.error = error.message;
        } finally { polling = false; }
    }
    var fields = [
        { id: 'elevationOffset', min: -64, max: 96 },
        { id: 'reliefPercent', min: 25, max: 250 },
        { id: 'spacingPercent', min: 40, max: 240 },
        { id: 'shapePercent', min: 25, max: 250 },
        { id: 'shoreIceBlocks', min: 0, max: 24 },
        { id: 'generationChancePercent', min: 0, max: 200 },
        { id: 'biomeSizePercent', min: 40, max: 240 }
    ];

    function label(key) {
        return state.labels[key] || key;
    }

    function commitChunks(value) {
        if (!String(value).trim()) {
            chunkInput.value = String(state.previewChunks);
            return;
        }
        var next = Math.max(state.minPreviewChunks, Math.round(Number(value) / 2) * 2);
        if (!Number.isSafeInteger(next)) return;
        chunkInput.value = String(next);
        if (next === state.previewChunks) return;
        state.previewChunks = next;
        send('chunks', { value: next });
    }

    var previewPointer;
    function previewPointerDown(event) {
        if (event.button > 2) return;
        event.preventDefault(); event.currentTarget.focus();
        event.currentTarget.setPointerCapture(event.pointerId);
        previewPointer = { id: event.pointerId, button: event.button, x: event.clientX, y: event.clientY };
    }
    function previewPointerMove(event) {
        if (!previewPointer || previewPointer.id !== event.pointerId) return;
        var rect = event.currentTarget.getBoundingClientRect();
        var dx = event.clientX - previewPointer.x, dy = event.clientY - previewPointer.y;
        previewPointer.x = event.clientX; previewPointer.y = event.clientY;
        if (previewPointer.button === 2) camera({ rotationDelta: dx * 0.006, angleDelta: dy * 0.006 });
        else camera({ panX: dx / rect.width, panZ: -dy / rect.height });
    }
    function previewPointerUp(event) {
        if (!previewPointer || previewPointer.id !== event.pointerId) return;
        previewPointer = undefined;
        if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
    }
    function previewWheel(event) {
        event.preventDefault();
        camera({ zoomFactor: Math.exp(Math.max(-1, Math.min(1, event.deltaY * 0.0015))) });
    }
    function previewKey(event) {
        if (event.key === 'ArrowLeft') camera({ panX: 0.06 });
        else if (event.key === 'ArrowRight') camera({ panX: -0.06 });
        else if (event.key === 'ArrowUp') camera({ panZ: 0.06 });
        else if (event.key === 'ArrowDown') camera({ panZ: -0.06 });
        else if (event.key === '+' || event.key === '=') camera({ zoomFactor: 0.85 });
        else if (event.key === '-') camera({ zoomFactor: 1 / 0.85 });
        else if (event.key === 'Home') resetCamera();
        else return;
        event.preventDefault();
    }
    function send(action, values) {
        if (action === 'rotate') { camera({ rotationDelta: values.direction === 'left' ? -0.2 : 0.2 }); return; }
        if (action === 'zoom') { camera({ zoomFactor: values.direction === 'in' ? 0.85 : 1 / 0.85 }); return; }
        var command = { action: action, inputAt: Date.now() };
        var key;
        if (values) {
            for (key in values) {
                if (Object.prototype.hasOwnProperty.call(values, key)) command[key] = values[key];
            }
        }
        if (action === 'set' || action === 'chunks') {
            command.sequence = ++sequence;
            pending[action === 'set' ? command.field : 'chunks'] = command.sequence;
            for (var i = commands.length - 1; i >= 0; i--) {
                if (commands[i].action === action && (action !== 'set' || commands[i].field === command.field)) {
                    commands.splice(i, 1);
                }
            }
        }
        if (action === 'reset' || action === 'select') pending = {};
        if (action === 'view' || action === 'viewport' || action === 'tooltips') {
            for (var j = commands.length - 1; j >= 0; j--) if (commands[j].action === action) commands.splice(j, 1);
        }
        commands.push(command);
        post();
    }

    function receive() {
        var raw = bridge.getAttribute('data-state');
        if (!raw) return;
        var update = JSON.parse(raw);
        if (JSON.stringify(state.biomes) !== JSON.stringify(update.biomes)) state.biomes = update.biomes;
        var selectionChanged = state.selected !== update.selected;
        state.selected = update.selected;
        var acknowledged = update.acknowledged || {};
        fields.forEach(function (field) {
            if (selectionChanged || !pending[field.id] || acknowledged[field.id] >= pending[field.id]) {
                state.profile[field.id] = update.profile[field.id];
                delete pending[field.id];
            }
            state.defaults[field.id] = update.defaults[field.id];
        });
        state.seed = update.seed;
        state.serverLoading = update.loading;
        state.loading = update.loading || state.previewInstalledGeneration !== update.generation;
        state.previewAvailable = update.previewAvailable;
        state.detailed = update.detailed;
        state.macro = update.macro;
        state.wholePrecisionSupported = update.wholePrecisionSupported;
        state.realtimeReady = update.realtimeReady;
        state.wholeProgress = update.wholeProgress || 0;
        if (state.generation !== update.generation) state.detailReady = false;
        state.generation = update.generation;
        state.detailLoading = !state.detailReady && (update.detailLoading || update.detailPrepared);
        state.error = update.error;
        state.feedback = update.feedback;
        state.busy = update.busy;
        if (JSON.stringify(state.labels) !== JSON.stringify(update.labels)) state.labels = update.labels;
        if (!pending.chunks || acknowledged.chunks >= pending.chunks) {
            state.previewChunks = update.previewChunks;
            if (!document.activeElement || document.activeElement.id !== 'terrain-chunks-input') {
                chunkInput.value = String(update.previewChunks);
            }
            delete pending.chunks;
        }
        state.minPreviewChunks = update.minPreviewChunks;
        if (update.preview) {
            window.auiPreviewState = update.preview;
            state.previewInstalledGeneration = update.preview.sceneKey;
            state.detailReady = update.preview.detailHires === true;
            state.loading = update.loading || update.preview.sceneKey !== update.generation;
            if (state.detailReady) state.detailLoading = false;
        }
        if (selectionChanged) requestAnimationFrame(resetCamera);
    }

    var watcher = new MutationObserver(receive);
    watcher.observe(bridge, { attributes: true, attributeFilter: ['data-state'] });

    function button(title, action, variant, values, id, disabled) {
        var control = h(McUIVue.McButton, {
            id: id,
            key: id,
            size: 'small',
            variant: variant,
            disabled: disabled || state.busy,
            onClick: function () { send(action, values); }
        }, { default: function () { return title; } });
        var tips = { save: 'saveTip', close: 'cancelTip', export: 'exportTip', reset: 'resetTip' };
        if (!tips[action]) return control;
        return h(McUIVue.McTooltip, {
            content: label(tips[action]),
            placement: action === 'close' ? 'bottom' : 'top',
            delay: 250,
            class: 'terrain-action-tooltip'
        }, { default: function () { return control; } });
    }

    function selectedName() {
        for (var i = 0; i < state.biomes.length; i++) {
            if (state.biomes[i].value === state.selected) return state.biomes[i].title;
        }
        return state.selected;
    }

    var ParameterControl = Vue.defineComponent({
        props: ['field'],
        setup: function (props) {
            return function () {
                var field = props.field;
        return h('div', { class: 'terrain-field', key: field.id, id: 'terrain-field-' + field.id }, [
            h(McUIVue.McSlider, {
                label: label(field.id),
                modelValue: state.profile[field.id],
                min: field.min,
                max: field.max,
                step: 1,
                showValue: true,
                disabled: state.busy,
                'onUpdate:modelValue': function (value) {
                    state.profile[field.id] = Number(value);
                    send('set', { field: field.id, value: Number(value) });
                }
            }),
            h('p', { class: 'terrain-default' }, label('default') + ': ' +
                (state.defaults[field.id] === undefined ? '—' : state.defaults[field.id]))
        ]);
            };
        }
    });

    function slider(field) {
        return h(ParameterControl, { field: field, key: field.id });
    }

    function rangeButton(direction, id) {
        return h(McUIVue.McButton, {
            id: id, key: id, size: 'small', variant: 'normal',
            disabled: state.busy || direction < 0 && state.previewChunks <= state.minPreviewChunks,
            onClick: function () {
                var next = Math.max(state.minPreviewChunks, state.previewChunks + direction * 2);
                state.previewChunks = next;
                send('chunks', { value: next });
            }
        }, { default: function () { return direction < 0 ? '−' : '+'; } });
    }

    var app = Vue.createApp({
        render: function () {
            var controls = [
                h('div', { class: 'terrain-selector' }, [
                    h('div', { class: 'terrain-selector-label' }, label('biome')),
                    h(McUIVue.McButton, {
                        id: 'terrain-biome-toggle',
                        class: 'terrain-picker-trigger',
                        variant: 'normal',
                        disabled: state.busy,
                        'aria-expanded': menuOpen.value,
                        onClick: function () { menuOpen.value = !menuOpen.value; }
                    }, { default: function () { return selectedName(); } }),
                     h('span', { class: 'terrain-seed' }, label('seed') + ': ' + state.seed),
                     menuOpen.value ? h('div', { class: 'terrain-biome-list' }, state.biomes.map(function (biome) {
                        return h(McUIVue.McButton, {
                            id: 'terrain-biome-' + biome.value,
                            key: biome.value,
                            disabled: state.busy,
                            variant: biome.value === state.selected ? 'primary' : 'normal',
                            onClick: function () {
                                send('select', { biome: biome.value });
                                menuOpen.value = false;
                            }
                        }, { default: function () { return biome.title; } });
                    })) : null
                ])
            ];
            fields.forEach(function (field) { controls.push(slider(field)); });
            return h(McUIVue.McApp, { class: 'terrain-app' }, {
                default: function () {
                    return h('div', { class: 'terrain-page' }, [
                        h(McUIVue.McHeader, { title: label('title'), class: 'terrain-header' }, {
                            right: function () {
                                return button(label('back'), 'close', 'normal', null, 'terrain-close-top');
                            }
                        }),
                        h('main', { class: 'terrain-main' }, [
                            h('div', { class: 'terrain-columns' }, [
                                h(McUIVue.McPanel, { title: label('parameters'), class: 'terrain-editor' }, {
                                    default: function () { return controls; }
                                }),
                                 h(McUIVue.McPanel, { title: selectedName() + ' · ' + label('preview'), class: 'terrain-stage' }, {
                                     default: function () {
                                         return [
                                             h('div', { class: 'terrain-view-actions' }, [
                                                 h('div', { class: 'terrain-preview-size' }, [
                                                     h('span', null, label('chunks')),
                                                     rangeButton(-1, 'terrain-chunks-less'),
                                                     h(McUIVue.McTextField, {
                                                         id: 'terrain-chunks-input', class: 'terrain-chunks-input',
                                                         type: 'text', filter: 'number', inputmode: 'numeric',
                                                         disabled: state.busy,
                                                         modelValue: chunkInput.value, 'aria-label': label('chunks'),
                                                         'onUpdate:modelValue': function (value) {
                                                             chunkInput.value = value;
                                                             var count = Number(value);
                                                             if (Number.isSafeInteger(count) && count >= state.minPreviewChunks && count % 2 === 0) commitChunks(value);
                                                         },
                                                         onKeydown: function (event) {
                                                             if (event.key === 'Enter') {
                                                                 event.preventDefault();
                                                                 commitChunks(chunkInput.value);
                                                                 event.target.blur();
                                                             }
                                                         },
                                                         onChange: commitChunks
                                                     }),
                                                     h('span', { id: 'terrain-chunks-size' }, '× ' + state.previewChunks),
                                                     rangeButton(1, 'terrain-chunks-more')
                                                 ]),
                                                 h('div', { class: 'terrain-camera-actions' }, [
                                                     button(label('left'), 'rotate', 'normal', { direction: 'left' }, 'terrain-rotate-left'),
                                                     button(label('right'), 'rotate', 'normal', { direction: 'right' }, 'terrain-rotate-right'),
                                                     button('−', 'zoom', 'normal', { direction: 'out' }, 'terrain-zoom-out'),
                                                     button('+', 'zoom', 'normal', { direction: 'in' }, 'terrain-zoom-in')
                                                 ])
                                             ]),
                                             h('div', { id: 'terrain-preview', class: 'terrain-preview', tabindex: 0,
                                                 role: 'application', 'aria-label': label('preview'),
                                                 onPointerdown: previewPointerDown, onPointermove: previewPointerMove,
                                                 onPointerup: previewPointerUp, onPointercancel: previewPointerUp,
                                                 onContextmenu: function (event) { event.preventDefault(); },
                                                 onWheel: previewWheel, onKeydown: previewKey }),
                                             h('div', { class: 'terrain-readout', 'aria-live': 'polite' }, [
                                                 h('p', { class: 'terrain-note' }, label('help')),
                                                 state.wholePrecisionSupported ? h('p', { class: 'terrain-status' },
                                                     state.detailReady ? label('wholeReady') : state.realtimeReady ? label('realtime')
                                                         : label('wholeLoading') + ' ' + Math.floor(state.wholeProgress * 100) + '%')
                                                     : state.detailLoading ? h('p', { class: 'terrain-status' }, label('detailLoading'))
                                                     : state.detailReady ? h('p', { class: 'terrain-status' }, label('detailReady'))
                                                     : state.loading ? h('p', { class: 'terrain-status' },
                                                         label(state.macro ? 'macro' : state.previewAvailable ? 'refining' : 'loading'))
                                                     : state.macro ? h('p', { class: 'terrain-status' }, label('macro')) : null,
                                                 state.error ? h('p', { class: 'terrain-status terrain-error' }, state.error) : null,
                                                 state.feedback ? h('p', { class: 'terrain-status' }, state.feedback) : null
                                             ])
                                         ];
                                     }
                                 })
                            ])
                        ]),
                        h('footer', { class: 'terrain-footer' }, [
                            h('div', { class: 'terrain-footer-left' }, [
                                button(label('reset'), 'reset', 'error', null, 'terrain-reset')
                            ]),
                            h('div', { class: 'terrain-footer-right' }, [
                                button(label('export'), 'export', 'normal', null, 'terrain-export'),
                                button(label('done'), 'save', 'primary', null, 'terrain-save')
                            ])
                        ])
                    ]);
                }
            });
        }
    });
    app.use(McUIVue.createMcUI({ sounds: { enabled: false } }));
    app.mount('#app');
    var nativeHost = window.chrome && window.chrome.webview;
    var lastBounds = '';
    var lastTooltipBounds = '';
    var notifyScheduled = false;
    function notifyNative() {
        if (notifyScheduled) return;
        notifyScheduled = true;
        requestAnimationFrame(function () {
            notifyScheduled = false;
            var frame = document.getElementById('terrain-preview');
            if (frame) {
                var rect = frame.getBoundingClientRect();
                var bounds = [(rect.x + frame.clientLeft) / innerWidth,
                    (rect.y + frame.clientTop) / innerHeight,
                    frame.clientWidth / innerWidth, frame.clientHeight / innerHeight];
                var signature = bounds.join('|');
                if (signature !== lastBounds) {
                    lastBounds = signature;
                    send('viewport', { x: bounds[0], y: bounds[1], width: bounds[2], height: bounds[3] });
                }
            }
            var tips = [];
            document.querySelectorAll('.mc-tooltip__content').forEach(function (tip) {
                var rect = tip.getBoundingClientRect();
                var style = getComputedStyle(tip);
                if (rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden') {
                    tips.push({ x: rect.x / innerWidth, y: rect.y / innerHeight,
                        width: rect.width / innerWidth, height: rect.height / innerHeight });
                }
            });
            var tooltipSignature = JSON.stringify(tips);
            if (tooltipSignature !== lastTooltipBounds) {
                lastTooltipBounds = tooltipSignature;
                send('tooltips', { regions: tips });
            }
            if (nativeHost) nativeHost.postMessage('kui-ui-dirty');
        });
    }
    {
        new MutationObserver(notifyNative).observe(document.getElementById('app'),
            { attributes: true, childList: true, characterData: true, subtree: true });
        function containsTooltip(node) {
            return node.nodeType === 1 && (node.matches('.mc-tooltip__content')
                || node.querySelector('.mc-tooltip__content') !== null);
        }
        new MutationObserver(function (changes) {
            for (var i = 0; i < changes.length; i++) {
                var change = changes[i];
                if (containsTooltip(change.target) || Array.from(change.addedNodes).some(containsTooltip)
                    || Array.from(change.removedNodes).some(containsTooltip)) {
                    notifyNative();
                    return;
                }
            }
        }).observe(document.body, { attributes: true, childList: true, characterData: true, subtree: true });
        new ResizeObserver(notifyNative).observe(document.getElementById('terrain-preview'));
        document.addEventListener('input', notifyNative);
        document.addEventListener('focusin', notifyNative);
        document.addEventListener('focusout', notifyNative);
        document.addEventListener('pointerdown', notifyNative);
        document.addEventListener('pointerover', notifyNative);
        document.addEventListener('pointerup', notifyNative);
        window.addEventListener('resize', notifyNative);
        notifyNative();
    }
    document.addEventListener('keydown', function (event) { if (event.key === 'Escape') send('close'); });
    document.addEventListener('focusout', function (event) {
        if (event.target.id === 'terrain-chunks-input') commitChunks(chunkInput.value);
    });
    poll();
    setInterval(poll, 16);
})();
