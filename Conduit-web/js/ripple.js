/**
 * ripple.js - Background-Confined & Battery Optimized
 */
const RIPPLE_CONFIG = {
    STEP: 2, 
    FRICTION: 0.97,
    VELOCITY: 10.0,
    INITIAL_AMP: 100,
    MAX_RADIUS: 1000,
    MIN_AMPLITUDE: 1.0,
    WAVE_LENGTH: 0.15,
    FADE_EXPONENT: 5.0,
    MIN_INTERVAL: 500 
};

let canvas, ctx, img, referenceData, outputImageData, outputBuffer;
let ripples = [];
let LastRippleTime = 0;

function initRippleEngine() {
    canvas = document.getElementById('rippleCanvas');
    ctx = canvas.getContext('2d', { alpha: false });
    
    canvas.addEventListener('mousedown', (e) => { triggerRipple(e); });

    img = new Image();
    img.src = 'img/Conduit-bg.webp';
    img.onload = () => {
        const w = canvas.width = img.naturalWidth;
        const h = canvas.height = img.naturalHeight;
        
        // Create an off-screen buffer to hold the clean background
        const tempCanvas = document.createElement('canvas');
        tempCanvas.width = w;
        tempCanvas.height = h;
        const tempCtx = tempCanvas.getContext('2d');
        tempCtx.drawImage(img, 0, 0);
        
        const rawData = tempCtx.getImageData(0, 0, w, h).data;
        referenceData = new Uint32Array(rawData.buffer);
        
        // Initialize the persistent output buffer
        outputImageData = ctx.createImageData(w, h);
        outputBuffer = new Uint32Array(outputImageData.data.buffer);
        
        requestAnimationFrame(renderLoop);
    };
}

function triggerRipple(e) {
    const now = Date.now();
    if (now - LastRippleTime < RIPPLE_CONFIG.MIN_INTERVAL) return;
    LastRippleTime = now;
    
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    
    ripples.push({
        x: (e.clientX - rect.left) * scaleX,
        y: (e.clientY - rect.top) * scaleY,
        progress: 0,
        amplitude: RIPPLE_CONFIG.INITIAL_AMP
    });
    
    new Audio('./audio/bubble.wav').play().catch(() => {});
}

function renderLoop() {
    if (ripples.length === 0) {
        ctx.drawImage(img, 0, 0);
        requestAnimationFrame(renderLoop);
        return;
    }

    const w = canvas.width;
    const h = canvas.height;
    const s = RIPPLE_CONFIG.STEP;

    // Reset the working buffer to the clean background image
    outputBuffer.set(referenceData);

    for (let y = 0; y < h; y += s) {
        for (let x = 0; x < w; x += s) {
            let dX = 0, dY = 0, active = false;
            
            for (let i = 0; i < ripples.length; i++) {
                const r = ripples[i];
                const dx = x - r.x;
                const dy = y - r.y;
                const distSq = dx * dx + dy * dy;
                
                if (distSq > 3240000) continue; // Pre-calculated 1800 * 1800

                const dist = Math.sqrt(distSq);
                const diff = dist - r.progress;

                if (diff > -250 && diff < 0) {
                    active = true;
                    const force = (Math.sin(diff * 0.15) * r.amplitude * Math.pow(1 + diff / 250, 5) * (1 - dist / 1800)) / (dist || 1);
                    dX += dx * force;
                    dY += dy * force;
                }
            }

            if (active) {
                const tx = Math.min(w - 1, Math.max(0, (x + dX) | 0));
                const ty = Math.min(h - 1, Math.max(0, (y + dY) | 0));
                const color = referenceData[ty * w + tx];

                // Apply the displaced color to the block
                for (let ky = 0; ky < s && (y + ky) < h; ky++) {
                    const offset = (y + ky) * w;
                    for (let kx = 0; kx < s && (x + kx) < w; kx++) {
                        outputBuffer[offset + (x + kx)] = color;
                    }
                }
            }
        }
    }

    ctx.putImageData(outputImageData, 0, 0);
    updateRipplePhysics();
    requestAnimationFrame(renderLoop);
}

function updateRipplePhysics() {
    for (let i = ripples.length - 1; i >= 0; i--) {
        ripples[i].progress += RIPPLE_CONFIG.VELOCITY;
        ripples[i].amplitude *= RIPPLE_CONFIG.FRICTION;
        if (ripples[i].amplitude < 1.0 || ripples[i].progress > 1800) {
            ripples.splice(i, 1);
        }
    }
}

window.triggerRipple = triggerRipple;
window.initRippleEngine = initRippleEngine;
