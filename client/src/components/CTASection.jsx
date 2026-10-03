import React from 'react';
import { Link } from 'react-router-dom';
import '../styles/LandingPage.css';

const CTASection = () => (
  <section className="cta-section primary-bg">
    <div className="container cta-content">
      <h2>Ready to Find Your Sports Partner?</h2>
      <p>Join thousands of athletes who\'ve found their perfect workout buddy</p>
      <Link to="/signup" className="btn btn-primary btn-lg">Start Matching Today</Link>
    </div>
  </section>
);

export default CTASection;
