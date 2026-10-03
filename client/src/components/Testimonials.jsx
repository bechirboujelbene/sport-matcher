import React from 'react';
import '../styles/LandingPage.css';

const testimonials = [
  {
    quote: 'Found my running buddy within a week! We\'ve completed 3 marathons together.',
    name: 'Sarah Chan',
    role: 'Marathon Runner',
    avatar: `${import.meta.env.BASE_URL}images/avatar1.png`
  },
  {
    quote: 'As someone new to the city, this app helped me find my cycling group!',
    name: 'Mike Rodriguez',
    role: 'Cyclist',
    avatar: `${import.meta.env.BASE_URL}images/avatar2.png`
  },
  {
    quote: 'Perfect for finding hiking partners who match my pace and experience level.',
    name: 'Emma Johnson',
    role: 'Hiker',
    avatar: `${import.meta.env.BASE_URL}images/avatar3.png`
  }
];

const Testimonials = () => {
  return (
    <section className="testimonials">
      <div className="container">
        <h2>What Our Users Say</h2>
        <div className="testimonial-grid">
          {testimonials.map((t, idx) => (
            <blockquote key={idx} className="testimonial">
              <p>"{t.quote}"</p>
              <div className="user-info">
                <img src={t.avatar} alt={t.name} className="avatar" />
                <div>
                  <strong>{t.name}</strong>
                  <span className="role">{t.role}</span>
                </div>
              </div>
            </blockquote>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Testimonials;
