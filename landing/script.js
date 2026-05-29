/* GasApp landing — interacciones y animaciones */
(function () {
  'use strict';

  /* Año en el footer */
  var yearEl = document.getElementById('year');
  if (yearEl) yearEl.textContent = new Date().getFullYear();

  /* Nav: sombra al hacer scroll + menú móvil */
  var nav = document.getElementById('nav');
  var toggle = document.getElementById('navToggle');

  function onScroll() {
    if (window.scrollY > 8) nav.classList.add('scrolled');
    else nav.classList.remove('scrolled');
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  if (toggle) {
    toggle.addEventListener('click', function () {
      nav.classList.toggle('open');
    });
    /* Cierra el menú al pulsar un enlace */
    nav.querySelectorAll('.nav-links a, .nav-cta').forEach(function (a) {
      a.addEventListener('click', function () { nav.classList.remove('open'); });
    });
  }

  /* Revelado al hacer scroll (robusto ante scroll rápido) */
  var reveals = Array.prototype.slice.call(document.querySelectorAll('.reveal'));

  function show(el) {
    if (el.classList.contains('in')) return;
    var delay = parseInt(el.getAttribute('data-delay') || '0', 10);
    setTimeout(function () { el.classList.add('in'); }, delay);
  }

  if ('IntersectionObserver' in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        // Revela tanto al entrar como si ya quedó por encima del viewport
        // (evita que un scroll rápido deje secciones invisibles).
        if (entry.isIntersecting || entry.boundingClientRect.top < 0) {
          show(entry.target);
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -8% 0px' });
    reveals.forEach(function (el) { io.observe(el); });

    /* Red de seguridad: cualquier elemento que ya esté en pantalla o por
       encima se revela aunque el observer no haya disparado a tiempo. */
    var ticking = false;
    function sweep() {
      ticking = false;
      var h = window.innerHeight;
      reveals.forEach(function (el) {
        if (el.classList.contains('in')) return;
        if (el.getBoundingClientRect().top < h * 0.92) show(el);
      });
    }
    window.addEventListener('scroll', function () {
      if (!ticking) { ticking = true; requestAnimationFrame(sweep); }
    }, { passive: true });
    window.addEventListener('load', sweep);
    sweep();
  } else {
    reveals.forEach(function (el) { el.classList.add('in'); });
  }

  /* Contadores animados */
  function animateCount(el) {
    var target = parseFloat(el.getAttribute('data-count'));
    var suffix = el.getAttribute('data-suffix') || '';
    var dur = 1400, start = null;
    function step(ts) {
      if (!start) start = ts;
      var p = Math.min((ts - start) / dur, 1);
      var eased = 1 - Math.pow(1 - p, 3); /* easeOutCubic */
      var val = Math.floor(eased * target);
      el.textContent = val.toLocaleString('es-ES') + suffix;
      if (p < 1) requestAnimationFrame(step);
      else el.textContent = target.toLocaleString('es-ES') + suffix;
    }
    requestAnimationFrame(step);
  }

  var counters = document.querySelectorAll('[data-count]');
  if ('IntersectionObserver' in window) {
    var io2 = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) { animateCount(entry.target); io2.unobserve(entry.target); }
      });
    }, { threshold: 0.6 });
    counters.forEach(function (el) { io2.observe(el); });
  } else {
    counters.forEach(animateCount);
  }

  /* Parallax sutil de los teléfonos del hero */
  var heroPhone = document.querySelector('.hero-phone');
  if (heroPhone && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    window.addEventListener('scroll', function () {
      var y = window.scrollY;
      if (y < 700) heroPhone.style.transform = 'translateY(' + (y * 0.04) + 'px)';
    }, { passive: true });
  }
})();
